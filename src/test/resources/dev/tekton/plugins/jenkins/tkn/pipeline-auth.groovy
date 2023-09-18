node {
    withKubeConfig([credentialsId: 'test-kubeconfig', serverUrl: 'https://api.test-k8s:6443' ]) {
        tkn toolVersion: 'v0.31.1', commands: 'version'
    }
}