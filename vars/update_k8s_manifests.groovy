def call(Map config = [:]) {
    def imageTag = config.imageTag ?: error("Image tag is required")
    def manifestsPath = config.manifestsPath ?: 'kubernetes'
    def gitCredentials = config.gitCredentials ?: 'github-credentials'
    def gitUserName = config.gitUserName ?: 'Arman076'
    def gitUserEmail = config.gitUserEmail ?: 'khanarmankh121@gmail.com'
    def gitBranch = config.gitBranch ?: 'main'  // changed default
    def dockerCredentials = config.dockerCredentials ?: 'docker-hub-credentials '
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

        // Update ingress host
        sh """
            if [ -f "${manifestsPath}/10-ingress.yaml" ]; then
                sed -i "s|host: .*|host: easyshop.letsdeployit.com|g" ${manifestsPath}/10-ingress.yaml
            fi
        """

        // Commit & push
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
