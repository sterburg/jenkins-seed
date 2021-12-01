pipeline {
	agent {
		label 'master'
	}
	stages {
		stage('Run job seed') {
			steps {
				wrap([$class: 'AnsiColorBuildWrapper', 'colorMapName': 'XTerm']) {
					jobDsl failOnSeedCollision: true, removedConfigFilesAction: 'DELETE', removedJobAction: 'DELETE', removedViewAction: 'DELETE', targets: '**/*.groovy'
				}
			}
		}
	}
}
