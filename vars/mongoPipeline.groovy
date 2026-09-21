def call() {
    pipeline {
        agent any

        stages {
            stage('Init') {
                steps {
                    script {
                        // Load config.properties from the shared library resources
                        def configText = libraryResource('config.properties')
                        def config = readProperties text: configText

                        // Save config to env for later stages
                        env.SLACK_CHANNEL_NAME = config.SLACK_CHANNEL_NAME
                        env.ENVIRONMENT        = config.ENVIRONMENT
                        env.CODE_BASE_PATH     = config.CODE_BASE_PATH
                        env.ACTION_MESSAGE     = config.ACTION_MESSAGE
                        env.KEEP_APPROVAL_STAGE = config.KEEP_APPROVAL_STAGE
                    }
                }
            }

            stage('Clone') {
                steps {
                    git url: 'https://github.com/siwach9500/ansible-jenkins-shared-library.git',
                        branch: env.ENVIRONMENT
                }
            }

            stage('User Approval') {
                when {
                    expression { env.KEEP_APPROVAL_STAGE.toBoolean() }
                }
                steps {
                    input message: "Approve MongoDB deployment to ${env.ENVIRONMENT}?"
                }
            }

            stage('Playbook Execution') {
                steps {
                    sh "ansible-playbook -i ${env.CODE_BASE_PATH}/hosts.ini site.yml --vault-password-file ${env.CODE_BASE_PATH}/vault.yml"
                }
            }


            stage('Notification') {
                steps {
                    slackSend(
                        channel: "#${env.SLACK_CHANNEL_NAME}",
                        message: "${env.ACTION_MESSAGE} on ${env.ENVIRONMENT}"
                    )
                }
            }
        }
    }
}
