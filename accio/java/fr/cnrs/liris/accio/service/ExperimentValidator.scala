/*
 * Accio is a platform to launch computer science experiments.
 * Copyright (C) 2016-2018 Vincent Primault <v.primault@ucl.ac.uk>
 *
 * Accio is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * Accio is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with Accio.  If not, see <http://www.gnu.org/licenses/>.
 */

package fr.cnrs.liris.accio.service

import com.google.inject.{Inject, Singleton}
import fr.cnrs.liris.accio.api.DataTypes
import fr.cnrs.liris.accio.api.thrift.{Experiment, FieldViolation}
import fr.cnrs.liris.accio.service
import fr.cnrs.liris.accio.storage.Storage

/**
 * Experiment validator.
 *
 * @param storage Storage.
 */
@Singleton
final class ExperimentValidator @Inject()(storage: Storage) {
  /**
   * Validate the definition of an experiment.
   *
   * @param experiment Experiment to validate.
   */
  def validate(experiment: Experiment): ValidationResult = {
    val builder = new service.ValidationResult.Builder
    validate(experiment, builder)
    builder.build
  }

  /**
   * Validate the definition of an experiment.
   *
   * @param experiment Experiment to validate.
   * @param builder    Validation result builder.
   */
  def validate(experiment: Experiment, builder: ValidationResult.Builder): Unit = {
    // Check the repeat parameter is correct.
    experiment.repeat.foreach { repeat =>
      if (repeat <= 0) {
        builder.error(FieldViolation(s"Number of times must be >= 1", "repeat"))
      }
    }

    storage.read(_.workflows.get(experiment.pkg.workflowId, experiment.pkg.workflowVersion)) match {
      case None =>
        builder.error(FieldViolation(
          s"Workflow not found: ${experiment.pkg.workflowId}@${experiment.pkg.workflowVersion}",
          "pkg"))
      case Some(workflow) =>
        // Check that all non-optional workflow parameters are defined.
        workflow.params.foreach { paramDef =>
          if (paramDef.defaultValue.isEmpty && !experiment.params.contains(paramDef.name)) {
            builder.error(FieldViolation(s"Required workflow param is missing: ${paramDef.name}", "params"))
          }
        }

        // Check that workflow parameters referenced actually exist.
        experiment.params.foreach { case (name, values) =>
          workflow.params.find(_.name == name) match {
            case None => builder.error(FieldViolation("Unknown workflow param", s"params.$name"))
            case Some(paramDef) =>
              values.zipWithIndex.foreach { case (value, idx) =>
                if (!DataTypes.isMaybe(paramDef.kind, value.kind)) {
                  builder.error(FieldViolation(
                    s"Data type mismatch: requires ${DataTypes.stringify(paramDef.kind)}, got ${DataTypes.stringify(value.kind)}",
                    s"params.$name.$idx"))
                }
              }
          }
        }
    }
  }
}
