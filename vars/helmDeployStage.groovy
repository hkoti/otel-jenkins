
def call(Map config) {
    stage('5. Deploy to Dev Kubernetes') {
        steps {
            script {
                def helmCommand = """
                    %HELM_EXE% upgrade --install ${config.serviceName} "${env.HELM_CHART_PATH}" ^
                    --namespace default ^
                    --set image.repository=${env.DOCKER_IMAGE_NAME} ^
                    --set image.tag=${env.DOCKER_IMAGE_TAG} ^
                    --wait
                """
                if (config.helmHasDependencies) {
                    bat "%HELM_EXE% dependency update \"${env.HELM_CHART_PATH}\""
                }
                bat helmCommand
            }
        }
    }
}
