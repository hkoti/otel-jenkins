// The Jenkinsfile for Day 4 - Definitive Version with Absolute Paths & Full Stages

pipeline {
    agent any

    environment {
        DOCKERHUB_USERNAME = "hkoti300"
        DOCKER_IMAGE_NAME  = "${DOCKERHUB_USERNAME}/opentelemetry-frontend"
        DOCKERFILE_PATH    = "C:/Users/heman/OneDrive/Desktop/DevOps/project/otel-jenkins/frontend/Dockerfile"
        HELM_CHART_PATH    = "C:/Users/heman/OneDrive/Desktop/DevOps/project/otel-jenkins/helm/frontend"
        
        // Definitive fix: Define the absolute path to docker.exe once
        DOCKER_EXE         = '"C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe"'
                // Absolute path to helm.exe - Discovered with 'which helm'
        HELM_EXE           = '"C:\\Users\\heman\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Helm.Helm_Microsoft.Winget.Source_8wekyb3d8bbwe\\windows-amd64\\helm.exe"'
    }
    
    // The tools block is still useful for Git
    tools {
        git 'git'
    }

    stages {

        stage('1. Checkout Source Code') {
            steps {
                cleanWs()
                git url: 'https://github.com/hkoti/opentelemetry-demo.git', branch: 'main'
            }
        }

        stage('2. SonarQube Code Analysis') {
            steps {
                script {
                    def safeWorkspace = env.WORKSPACE.replaceAll('\\\\', '/')
                    withSonarQubeEnv('sonar-server') {
                        bat """
                            %DOCKER_EXE% run --rm --network=host ^
                            -v "${safeWorkspace}:/usr/src/app" ^
                            -w /usr/src/app ^
                            sonarsource/sonar-scanner-cli:latest ^
                            sonar-scanner -Dsonar.projectKey=Otel-frontend -Dsonar.sources=src/frontend -Dsonar.host.url=http://localhost:9000 -Dsonar.login=%SONAR_AUTH_TOKEN%
                        """
                    }
                }
            }
        }

        stage('3. Build & Push Docker Image') {
            steps {
                script {
                    // --- THIS IS THE FINAL FIX ---
                    // We explicitly set the environment variable right before the command that needs it.
                    // This bypasses any inheritance issues with the script block.
                    bat "set DOCKER_CREDENTIALS_HELPER_DISABLED=1 && ${env.DOCKER_EXE} build -t ${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER} -f \"${env.DOCKERFILE_PATH}\" ."

                    withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                        bat """
                            %DOCKER_EXE% login -u %DOCKER_USER% -p %DOCKER_PASS%
                            %DOCKER_EXE% push ${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER}
                            %DOCKER_EXE% tag ${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER} ${env.DOCKER_IMAGE_NAME}:latest
                            %DOCKER_EXE% push ${env.DOCKER_IMAGE_NAME}:latest
                            %DOCKER_EXE% logout
                        """
                    }
                }
            }
        }

            stage('4. Trivy Vulnerability Scan') {
            steps {
                script {
                    // We use the absolute path to docker.exe for reliability.
                    def dockerExe = '"C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe"'
                    
                    // We use the 'bat' step on our Windows agent.
                    bat """
                        ${dockerExe} run --rm ^
                        -v "%WORKSPACE%:/app" ^
                        ghcr.io/aquasecurity/trivy:latest ^
                        image --exit-code 0 --format json --output /app/trivy-report.json ^
                        ${env.DOCKER_IMAGE_NAME}:latest
                    """
                }
                
                // After the scan is complete, we archive the report.
                // This makes the JSON file available from the Jenkins build page.
                archiveArtifacts artifacts: 'trivy-report.json', fingerprint: true
            }
        }
        
                stage('5. Deploy to Dev Kubernetes') {
            steps {
                // --- THIS IS THE FINAL FIX ---
                // We use the 'withCredentials' block to securely access our kubeconfig file.
                // It makes the file available at a temporary path and sets the KUBECONFIG
                // environment variable to point to it. Helm and kubectl automatically use this variable.
                withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KUBECONFIG')]) {
                    // This block executes with the KUBECONFIG environment variable set.
                    bat """
                        %HELM_EXE% upgrade --install frontend-dev "%HELM_CHART_PATH%" ^
                        --set image.tag=%BUILD_NUMBER% ^
                        --namespace dev --create-namespace
                    """
                }
            }
        }
    }

    post {
        success {
            echo '✅ SUCCESS: Code analyzed, image built, scanned, pushed, and deployed to Kubernetes.'
        }
        failure {
            echo '❌ FAILURE: Check console logs above for the stage that failed.'
        }
    }
}
