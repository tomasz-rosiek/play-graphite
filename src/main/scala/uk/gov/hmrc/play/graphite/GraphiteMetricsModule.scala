/*
 * Copyright 2017 HM Revenue & Customs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package uk.gov.hmrc.play.graphite

import com.codahale.metrics.MetricFilter
import com.codahale.metrics.graphite.{Graphite, GraphiteReporter}
import com.kenshoo.play.metrics._
import play.api.inject.{Binding, Module}
import play.api.{Configuration, Environment}

class GraphiteMetricsModule extends Module {

  override def bindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    if (configuration.getBoolean("microservice.metrics.graphite.legacy").getOrElse(true)) {
      legacy(environment, configuration)
    } else {
      newBindings(environment, configuration)
    }
  }

  private def legacy(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {
    if (metricsEnabled(configuration)) {
      Seq(
        bind[MetricsFilter].to[MetricsFilterImpl].eagerly,
        bind[Metrics].to[GraphiteMetricsImpl].eagerly
      )
    } else {
      Seq(
        bind[MetricsFilter].to[DisabledMetricsFilter].eagerly,
        bind[Metrics].to[DisabledMetrics].eagerly
      )
    }
  }

  private def newBindings(environment: Environment, configuration: Configuration): Seq[Binding[_]] = {

    val defaultBindings: Seq[Binding[_]] = Seq(
      // Note: `MetricFilter` rather than `MetricsFilter`
      bind[MetricFilter].toInstance(MetricFilter.ALL).eagerly
    )

    val metricsBindings: Seq[Binding[_]] =
      if (metricsEnabled(configuration)) {
        Seq(
          bind[MetricsFilter].to[MetricsFilterImpl].eagerly,
          bind[Metrics].to[MetricsImpl].eagerly,

          bind[GraphiteProviderConfig].toInstance(GraphiteProviderConfig.fromConfig(configuration)),
          bind[GraphiteReporterProviderConfig].toInstance(GraphiteReporterProviderConfig.fromConfig(configuration)),
          bind[Graphite].toProvider[GraphiteProvider],
          bind[GraphiteReporter].toProvider[GraphiteReporterProvider],
          bind[GraphiteReporting].to[EnabledGraphiteReporting].eagerly
        )
      } else {
        Seq(
          bind[MetricsFilter].to[DisabledMetricsFilter].eagerly,
          bind[Metrics].to[DisabledMetrics].eagerly,
          bind[GraphiteReporting].to[DisabledGraphiteReporting].eagerly
        )
      }

    defaultBindings ++ metricsBindings
  }

  private def metricsEnabled(configuration: Configuration) = {
    val metricsPluginEnabled = configuration.getBoolean("metrics.enabled").getOrElse(false)
    val graphitePublisherEnabled = configuration.getBoolean("microservice.metrics.graphite.enabled").getOrElse(false)
    metricsPluginEnabled && graphitePublisherEnabled
  }
}
