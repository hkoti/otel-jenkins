def call() {
    stage('1. Checkout Source Code') {
        steps {
            script {
                cleanWs()
                checkout scm
            }
        }
    }
}
