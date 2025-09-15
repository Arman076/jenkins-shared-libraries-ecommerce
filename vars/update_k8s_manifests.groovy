#!/usr/bin/env groovy

/**
 * Update Kubernetes manifests with new image tags and push to GitHub
 *
 * @param config Map of configuration options
 * config.imageTag       -> New Docker image tag (required)
 * config.manifestsPath  -> Path to K8s manifests (default: 'kubernetes')
 * config.gitCredentials -> Jenkins credential ID for GitHub (default: 'Github')
 * config.gitUserName    -> Git commit author name (default: 'Jenkins CI')
 * config.gitUserEmail   -> Git commit author email (default: 'jenkins@example.com')
 * config.gitBranch      -> Branch to push (default: 'main')
 * config.dockerCredentials -> Jenkins credential ID for Docker (default: 'docker-key')
 * config.dockerUsername    -> Docker username (default: 'devil678')
 */
def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'Github'
    def gitUserName = config.gitUserName ?: 'Arman076'
    def gitUserEmail = config.gitUserEmail ?: 'khanarmankh121@gmail.com'
    def gitBranch = config.gitBranch ?: 'main'
    def dockerCredentials = config.dockerCredentials ?: 'docker-key'
    def dockerUsername = config.dockerUsername ?: 'devil678'

    echo "Updating Kubernetes manifests with image tag: ${imageTag}"

    // Use GitHub credentials to push commits
    withCredentials([usernamePassword(
        credentialsId: gitCredentials,
        usernameVariable: 'GIT_USERNAME',
        passwordVariable: 'GIT_PASSWORD'
    )]) {

        // Configure Git commit author
        sh """
            git config user.name "${gitUserName}"
            git config user.email "${gitUserEmail}"
            
            git remote set-url origin https://\${GIT_USERNAME}:\${GIT_PASSWORD}@github.com/devil678/tws-e-commerce-app_hackathon.git
        """

        // Update main deployment manifest
        sh """
            sed -i "s|image: devil678/easyshop-app:.*|image: devil678/easyshop-app:${imageTag}|g" ${manifestsPath}/08-easyshop-deployment.yaml
        """

        // Update migration job if it exists
        sh """
            if [ -f "${manifestsPath}/12-migration-job.yaml" ]; then
                sed -i "s|image: devil678/easyshop-migration:.*|image: devil678/easyshop-migration:${imageTag}|g" ${manifestsPath}/12-migration-job.yaml
            fi
        """

        // Update ingress host if it exists
        sh """
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
        """

        // Commit and push changes
        sh """
            git add ${manifestsPath}/*.yaml

            if git diff --cached --quiet; then
                echo "No changes to commit"
            else
                git commit -m "Update image tags to ${imageTag} and ensure correct domain [ci skip]"
                git push origin HEAD:${gitBranch}
            fi
        """
    }

    // Optional: Docker login if you need to pull/push images
    withCredentials([usernamePassword(
        credentialsId: docker-key,
        usernameVariable: 'DOCKER_USERNAME',
        passwordVariable: 'DOCKER_PASSWORD'
    )]) {
        sh """
            echo "\$DOCKER_PASSWORD" | docker login -u "\$DOCKER_USERNAME" --password-stdin
            echo "Docker login successful for user ${dockerUsername}"
        """
    }
}
