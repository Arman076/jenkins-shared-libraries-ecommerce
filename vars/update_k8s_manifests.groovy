#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags and push to GitHub
 *
 * @param config Map of configuration options
 * config.imageTag       -> New Docker image tag (required)
 * config.manifestsPath  -> Path to K8s manifests (default: 'kubernetes')
 * config.gitCredentials -> Jenkins credential ID for GitHub (default: 'github-credentials')
 * config.gitUserName    -> Git commit author name (default: 'Arman076')
 * config.gitUserEmail   -> Git commit author email (default: 'khanarmankh121@gmail.com')
 * config.gitBranch      -> Branch to push (default: 'master')
 * config.dockerCredentials -> Jenkins credential ID for Docker (default: 'docker-hub-credentials')
 * config.dockerUsername    -> Docker username (default: 'devil678')
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Arman076'
    def gitUserEmail = config.gitUserEmail ?: 'khanarmankh121@gmail.com'
    def gitBranch = config.gitBranch ?: 'master'  // fixed default
    def dockerCredentials = config.dockerCredentials ?: 'docker-hub-credentials' // removed extra space
    def dockerUsername = config.dockerUsername ?: 'devil678'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    // Git push changes
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
        """

        // Update main deployment
        sh """
            sed -i "s|image: devil678/easyshop-app:.*|image: devil678/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
        """

        // Update migration job if exists
        sh """
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: devil678/easyshop-migration:.*|image: devil678/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
        """

        // Update ingress host if exists
        sh """
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
        """

        // Commit & push changes
        sh """
            git add ${manifestsPath}/*.yaml
            if git diff --cached --quiet; then
                echo "No changes to commit"
            else
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                git push https://${GIT_USERNAME}:${GIT_PASSWORD}@\$(git remote get-url origin | sed 's|https://||') HEAD:${gitBranch}
            fi
        """
    }

    // Docker login
    withCredentials([usernamePassword(
        credentialsId: dockerCredentials,
        usernameVariable: 'DOCKER_USERNAME',
        passwordVariable: 'DOCKER_PASSWORD'
    )]) {
        sh """
            echo "\$DOCKER_PASSWORD" | docker login -u "\$DOCKER_USERNAME" --password-stdin
            echo "Docker login successful for user ${dockerUsername}"
        """
    }
}
