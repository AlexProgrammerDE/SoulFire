plugins {
  checkstyle
}

checkstyle {
  maxErrors = 0
  maxWarnings = 0
  toolVersion = libs.checkstyle.get().version.toString()
}

// To fix a conflict in checkstyle dependencies
configurations.checkstyle {
  resolutionStrategy.capabilitiesResolution.withCapability("com.google.collections:google-collections") {
    select("com.google.guava:guava:${libs.guava.get().version}")
  }
}
