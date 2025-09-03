
def call(Map config) {
    stage('2. SonarQube Code Analysis') {
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
                            sonar-scanner -D"sonar.projectKey=${config.sonarProjectKey}" -D"sonar.sources=." -D"sonar.host.url=http://localhost:9000" -D"sonar.login=${SONAR_LOGIN_TOKEN}"
                        """
                    }
                }
            }
        }
    }
}
