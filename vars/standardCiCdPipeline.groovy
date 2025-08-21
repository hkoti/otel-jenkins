// The Definitive Shared Library - Based on the Proven Checkout Service Blueprint

def call(Map config) {
    pipeline {
        agent any

        environment {
            DOCKERHUB_USERNAME = "hkoti300"
            DOCKER_IMAGE_NAME  = "${DOCKERHUB_USERNAME}/${config.dockerImageName}"
            
            // Paths are constructed from the serviceName parameter
            DOCKERFILE_PATH    = "C:/Users/heman/OneDrive/desktop/DevOps/project/otel-jenkins/${config.serviceName}/Dockerfile"
            HELM_CHART_PATH    = "C:/Users/heman/OneDrive/desktop/DevOps/project/otel-jenkins/helm/${config.serviceName}"
            
            DOCKER_EXE         = '"C:\\Program Files\\Docker\\Docker\\resources\\bin\\docker.exe"'
            HELM_EXE           = '"C:\\Users\\heman\\AppData\\Local\\Microsoft\\WinGet\\Packages\\Helm.Helm_Microsoft.Winget.Source_8wekyb3d8bbwe\\windows-amd64\\helm.exe"'
        }
        
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
                // This stage is conditional. It will only run if a sonarProjectKey is provided.
                when { expression { return config.sonarProjectKey != null } }
                steps {
                    script {
                        def safeWorkspace = env.WORKSPACE.replaceAll('\\\\', '/')
                        withCredentials([string(credentialsId: config.sonarCredentialId, variable: 'SONAR_LOGIN_TOKEN')]) {
                            withSonarQubeEnv('sonar-server') {
                                bat """
                                    %DOCKER_EXE% run --rm --network=host ^
                                    -v "${safeWorkspace}:/usr/src/app" ^
                                    -w /usr/src/app/${config.sonarSourcesPath} ^
                                    sonarsource/sonar-scanner-cli:latest ^
                                    sonar-scanner ^
                                    -D"sonar.projectKey=${config.sonarProjectKey}" ^
                                    -D"sonar.sources=." ^
                                    -D"sonar.host.url=http://localhost:9000" ^
                                    -D"sonar.login=${SONAR_LOGIN_TOKEN}"
                                """
                            }
                        }
                    }
                }
            }

            stage('3. Build & Push Image') {
                steps {
                    script {
                        // We build and push the versioned tag and the 'latest' tag in one go.
                        bat "set DOCKER_CREDENTIALS_HELPER_DISABLED=1 && %DOCKER_EXE% build -t ${env.DOCKER_IMAGE_NAME}:${env.BUILD_NUMBER} -f \"${env.DOCKERFILE_PATH}\" ."

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
                    // We scan the 'latest' tag, which we know points to our just-built image.
                    // This is safe within a single pipeline execution.
                    bat """
                        %DOCKER_EXE% run --rm -v "%WORKSPACE%:/root/.cache/" ghcr.io/aquasecurity/trivy:latest image --exit-code 0 --severity HIGH,CRITICAL ${env.DOCKER_IMAGE_NAME}:latest
                    """
                }
            }

            stage('5. Deploy to Dev Kubernetes') {
                steps {
                    withCredentials([file(credentialsId: 'kubeconfig-file', variable: 'KUBECONFIG')]) {
                        if (config.helmHasDependencies) {
                            bat """
                                %HELM_EXE% repo add bitnami https://charts.bitnami.com/bitnami --force-update
                                %HELM_EXE% dependency update "%HELM_CHART_PATH%"
                            """
                        }
                        bat """
                            %HELM_EXE% upgrade --install ${config.serviceName}-dev "%HELM_CHART_PATH%" ^
                            --set image.tag=${env.BUILD_NUMBER} ^
                            --namespace dev --create-namespace
                        """
                    }
                }
            }
        }
        
        post {
            success {
                echo "✅ SUCCESS: ${config.serviceName} pipeline complete. Deployed to Kubernetes."
            }
            failure {
                echo "❌ FAILURE: ${config.serviceName} pipeline failed. Check console logs."
            }
        }
    }
}
