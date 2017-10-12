def greenColor = "#3BCF00"
def yellowColor = "#BFC14C"
def redColor = "#DE0000"

def buildNotify(buildStatus, channel) {
    // build status of null means successful
    buildStatus =  buildStatus ?: 'SUCCESSFUL'

    // Default values
    def colorCode = '#FF0000'
    def subject = "`[PROJECT]` ${buildStatus}: build branch `${env.BRANCH_NAME}`"
    def summary = "${subject} (<${env.BUILD_URL}|Open>)"

    // Override default values based on build status
    if (buildStatus == 'STARTED') {
      colorCode = yellowColor
    } else if (buildStatus == 'SUCCESSFUL') {
      colorCode = greenColor
    } else {
      colorCode = redColor
    }   

    // Send notifications
    // NOTE: you should get the token from Jenkins credentials instead of cleartext in script
    slackSend color: colorCode, message: summary, teamDomain: 'myDomain', channel: channel,token: 'myToken'
  }

node {
  def branchVersion = ""

  try {
    buildNotify 'STARTED', 'my-build-channel'
    
    stage ('Checkout') {
      // checkout repository
      checkout scm

      // save our docker build context before we switch branches
      sh "cp -r ./.docker/build tmp-docker-build-context"
    
      // checkout input branch 
      sh "git checkout ${caller.env.BRANCH_NAME}"
    }

    stage ('Determine Branch Version') {
      // add maven to path
      env.PATH = "${tool 'M3'}/bin:${env.PATH}"

      // determine version in pom.xml
      def pomVersion = sh(script: 'mvn -q -Dexec.executable=\'echo\' -Dexec.args=\'${project.version}\' --non-recursive exec:exec', returnStdout: true).trim()

      // compute proper branch SNAPSHOT version
      pomVersion = pomVersion.replaceAll(/-SNAPSHOT/, "") 
      branchVersion = env.BRANCH_NAME
      branchVersion = branchVersion.replaceAll(/origin\//, "") 
      branchVersion = branchVersion.replaceAll(/\W/, "-")
      branchVersion = "${pomVersion}-${branchVersion}-SNAPSHOT"

      // set branch SNAPSHOT version in pom.xml
      sh "mvn versions:set -DnewVersion=${branchVersion}"
    }

    stage ('Java Build') {
      // build .war package
      sh 'mvn clean package -U'
    }
  
    stage ('Docker Build') {
      // prepare docker build context
      sh "cp target/project.war ./tmp-docker-build-context"

      // Build and push image with Jenkins' docker-plugin
      withDockerServer([uri: "tcp://0.0.0.0:4243"]) {
        withDockerRegistry([credentialsId: 'YnVkOTM0MTE6ITQzTXlTZWxm', url: "https://0.0.0.0:4243"]) {
          // we give the image the same version as the .war package
          def image = docker.build("<myDockerRegistry>/<myDockerProjectRepo>:${branchVersion}", "--build-arg PACKAGE_VERSION=${branchVersion} ./tmp-docker-build-context")
          image.push()
        }   
      }
    } 
  } catch(e) {
    currentBuild.result = "FAILED"
    throw e
  } finally {
    buildNotify currentBuild.result, 'my-build-channel'
  }
}
