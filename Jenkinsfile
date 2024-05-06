@Library('socrata-pipeline-library')

import com.socrata.ReleaseMetadataService
def rmsSupportedEnvironment = com.socrata.ReleaseMetadataService.SupportedEnvironment

String service = 'query-coordinator'
String project_wd = service
boolean isPr = env.CHANGE_ID != null
boolean isHotfix = isHotfixBranch(env.BRANCH_NAME)
boolean skip = false
boolean lastStage

// Utility Libraries
def sbtbuild = new com.socrata.SBTBuild(steps, service, project_wd)
def dockerize = new com.socrata.Dockerize(steps, service, BUILD_NUMBER)
def releaseTag = new com.socrata.ReleaseTag(steps, service)

pipeline {
  options {
    ansiColor('xterm')
    buildDiscarder(logRotator(numToKeepStr: '50'))
    disableConcurrentBuilds(abortPrevious: true)
    timeout(time: 20, unit: 'MINUTES')
  }
  parameters {
    string(name: 'AGENT', defaultValue: 'build-worker', description: 'Which build agent to use?')
    string(name: 'BRANCH_SPECIFIER', defaultValue: 'origin/main', description: 'Use this branch for building the artifact.')
    booleanParam(name: 'RELEASE_BUILD', defaultValue: false, description: 'Are we building a release candidate?')
    booleanParam(name: 'RELEASE_DRY_RUN', defaultValue: false, description: 'To test out the release build without creating a new tag.')
    string(name: 'RELEASE_NAME', description: 'For release builds, the release name which is used for the git tag and the deploy tag.')
  }
  agent {
    label params.AGENT
  }
  environment {
    WEBHOOK_ID = 'WEBHOOK_IQ'
  }
  stages {
    stage('Hotfix Tag') {
      when {
        expression { isHotfix }
      }
      steps {
        script {
          lastStage = env.STAGE_NAME
          if (releaseTag.noCommitsOnHotfixBranch(env.BRANCH_NAME)) {
            skip = true
            echo "SKIP: Skipping the rest of the build because there are no commits on the hotfix branch yet"
            return
          }
          env.CURRENT_RELEASE_NAME = releaseTag.getReleaseName(env.BRANCH_NAME)
          env.HOTFIX_NAME = releaseTag.getHotfixName(env.CURRENT_RELEASE_NAME)
        }
      }
    }
    stage('Build') {
      when {
        not { expression { skip } }
      }
      steps {
        script {
          lastStage = env.STAGE_NAME
          sbtbuild.setScalaVersion("2.12")
          sbtbuild.setSubprojectName("queryCoordinator")
          sbtbuild.setSrcJar("query-coordinator/target/query-coordinator-assembly.jar")
          sbtbuild.build()
        }
      }
    }
    stage('Docker Build') {
      when {
        allOf {
          not { expression { isPr } }
          not { expression { skip } }
        }
      }
      steps {
        script {
          lastStage = env.STAGE_NAME
          if (params.RELEASE_BUILD || isHotfix) {
            env.VERSION = (isHotfix) ? env.HOTFIX_NAME : params.RELEASE_NAME
            env.DOCKER_TAG = dockerize.dockerBuildWithSpecificTag(
              tag: env.VERSION,
              path: sbtbuild.getDockerPath(),
              artifacts: [sbtbuild.getDockerArtifact()]
            )
          } else {
            env.DOCKER_TAG = dockerize.dockerBuildWithDefaultTag(
              version: 'STAGING',
              sha: env.GIT_COMMIT,
              path: sbtbuild.getDockerPath(),
              artifacts: [sbtbuild.getDockerArtifact()]
            )
          }
        }
      }
      post {
        success {
          script {
            if (isHotfix) {
              env.GIT_TAG = releaseTag.create(env.HOTFIX_NAME)
            } else if (params.RELEASE_BUILD) {
              env.GIT_TAG = releaseTag.getFormattedTag(params.RELEASE_NAME)
              if (releaseTag.doesReleaseTagExist(params.RELEASE_NAME)) {
                echo "REBUILD: Tag ${env.GIT_TAG} already exists"
                return
              }
              if (params.RELEASE_DRY_RUN) {
                echo "DRY RUN: Would have created ${env.GIT_TAG} and pushed it to the repo"
                currentBuild.description = "${service}:${params.RELEASE_NAME} - DRY RUN"
                return
              }
              releaseTag.create(params.RELEASE_NAME)
            }
          }
        }
      }
    }
    stage('Publish') {
      when {
        allOf {
          not { expression { isPr } }
          not { expression { skip } }
          not { expression { return params.RELEASE_BUILD && params.RELEASE_DRY_RUN } }
        }
      }
      steps {
        script {
          lastStage = env.STAGE_NAME
          if (isHotfix || params.RELEASE_BUILD) {
            env.BUILD_ID = dockerize.publish(sourceTag: env.DOCKER_TAG)
          } else {
            env.BUILD_ID = dockerize.publish(
              sourceTag: env.DOCKER_TAG,
              environments: ['internal']
            )
          }
          currentBuild.description = env.BUILD_ID
        }
      }
      post {
        success {
          script {
            if (isHotfix || params.RELEASE_BUILD) {
              env.PURPOSE = (isHotfix) ? 'hotfix' : 'initial'
              env.RELEASE_ID = (isHotfix) ? env.CURRENT_RELEASE_NAME : params.RELEASE_NAME
              Map buildInfo = [
                "project_id": service,
                "build_id": env.BUILD_ID,
                "release_id": env.RELEASE_ID,
                "git_tag": env.GIT_TAG,
                "purpose": env.PURPOSE,
              ]
              createBuild(
                buildInfo,
                rmsSupportedEnvironment.staging // production - for testing
              )
            }
          }
        }
      }
    }
    stage('Deploy') {
      when {
        not { expression { isPr } }
        not { expression { skip } }
        not { expression { return params.RELEASE_BUILD } }
      }
      steps {
        script {
          lastStage = env.STAGE_NAME
          env.ENVIRONMENT = (isHotfix) ? 'rc' : 'staging'
          marathonDeploy(
            serviceName: service,
            tag: env.BUILD_ID,
            environment: env.ENVIRONMENT
          )
        }
      }
      post {
        success {
          script {
            if (isHotfix) {
              Map deployInfo = [
                "build_id": env.BUILD_ID,
                "environment": env.ENVIRONMENT,
              ]
              createDeployment(
                deployInfo,
                rmsSupportedEnvironment.staging // production - for testing
              )
            }
          }
        }
      }
    }
  }
  post {
    failure {
      script {
        boolean buildingMain = (env.JOB_NAME.contains("${service}/main"))
        if (buildingMain) {
          teamsMessage(
            message: "[${currentBuild.fullDisplayName}](${env.BUILD_URL}) has failed in stage ${lastStage}",
            webhookCredentialID: WEBHOOK_ID
          )
        }
      }
    }
  }
}
