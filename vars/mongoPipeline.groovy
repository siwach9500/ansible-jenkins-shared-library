def call() {
    def config = readProperties file: "${libraryResource('config.properties')}"

    pipeline {
        agent any

        stages {
            stage('Clone') {
                steps {
                    git url: 'https://github.com/siwach9500/ansible-jenkins-shared-library.git',
                        branch: config.ENVIRONMENT
                }
            }

            stage('User Approval') {
                when {
                    expression { config.KEEP_APPROVAL_STAGE.toBoolean() }
                }
                steps {
                    input message: "Approve MongoDB deployment to ${config.ENVIRONMENT}?"
                }
            }

            stage('Playbook Execution') {
                steps {
                    ansiblePlaybook(
                        playbook: "roles/mongodb/tasks/main.yml",
                        inventory: "${config.CODE_BASE_PATH}/hosts.ini"
                    )
                }
            }

            stage('Notification') {
                steps {
                    slackSend(
                        channel: "#${config.SLACK_CHANNEL_NAME}",
                        message: "${config.ACTION_MESSAGE} on ${config.ENVIRONMENT}"
                    )
                }
            }
        }
    }
}
