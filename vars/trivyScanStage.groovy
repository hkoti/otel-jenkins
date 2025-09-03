
def call(Map config) {
    stage('4. Trivy Vulnerability Scan') {
        steps {
            script {
                bat """
                    %DOCKER_EXE% run --rm --network=host ^
                    aquasecurity/trivy:latest ^
                    client --remote http://localhost:4954 ${env.DOCKER_IMAGE_NAME}:${env.DOCKER_IMAGE_TAG}
                """
            }
        }
    }
}
