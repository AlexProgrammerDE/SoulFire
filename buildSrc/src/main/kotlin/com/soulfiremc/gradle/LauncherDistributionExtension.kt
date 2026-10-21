package com.soulfiremc.gradle

import org.gradle.api.provider.Property

abstract class LauncherDistributionExtension {
  abstract val mainClass: Property<String>
}
