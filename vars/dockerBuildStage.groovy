
def call(Map config) {
    stage('3. Build & Push Image') {
        steps {
            script {
                def dockerBuildArgs = config.dockerBuildArgs ?: '' // Default to empty string if not provided
                bat "set DOCKER_CREDENTIALS_HELPER_DISABLED=1 && %DOCKER_EXE% build ${dockerBuildArgs} -t ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_IMAGE_TAG} -f \"${env.DOCKERFILE_PATH}\" ."
                withCredentials([usernamePassword(credentialsId: 'dockerhub-credentials', usernameVariable: 'DOCKER_USER', passwordVariable: 'DOCKER_PASS')]) {
                    bat """
                        %DOCKER_EXE% login -u %DOCKER_USER% -p %DOCKER_PASS%
                        %DOCKER_EXE% push ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_IMAGE_TAG}
                        %DOCKER_EXE% logout
                    """
                }
            }
        }
    }
}
