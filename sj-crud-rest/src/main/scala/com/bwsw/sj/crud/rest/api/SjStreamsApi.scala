package com.bwsw.sj.crud.rest.api

import java.io.{File, FileNotFoundException}
import java.net.URI

import akka.http.scaladsl.model.MediaTypes._
import akka.http.scaladsl.model._
import akka.http.scaladsl.model.headers._
import akka.http.scaladsl.server.directives.FileInfo
import akka.http.scaladsl.server.{Directives, RequestContext}
import akka.stream.scaladsl._
import com.bwsw.common.exceptions.KeyAlreadyExists
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.DAL.service.GenericMongoService
import com.bwsw.sj.common.GeneratorConstants._
import com.bwsw.sj.crud.rest.entities._
import com.bwsw.sj.crud.rest.entities.SjStreamData
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.stream.StreamValidator

/**
  * Rest-api for streams
  *
  * Created by mendelbaum_nm
  */
trait SjStreamsApi extends Directives with SjCrudValidator {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val options = serializer.deserialize[SjStreamData](getEntityFromContext(ctx))
          val stream = generateStreamEntity(options)
          val errors = validateStream(stream, options)
          if (errors.isEmpty) {
            val nameStream = saveStream(stream)
            ctx.complete(HttpEntity(
              `application/json`,
              serializer.serialize(Response(200, nameStream, s"Stream '$nameStream' is created"))
            ))
          } else {
            throw new KeyAlreadyExists(s"Cannot create stream. Errors: ${errors.mkString("\n")}",
              s"${options.name}")
          }
        }
      }
    }
  }

  /**
    * Generate stream entity from stream data
    *
    * @param initialData - options for stream
    * @return - generated stream entity
    */
  def generateStreamEntity(initialData: SjStreamData) = {
    val stream = new SjStream
    stream.service = serviceDAO.get(initialData.service)
    stream.name = initialData.name
    stream.description = initialData.description
    stream.partitions = initialData.partitions
    stream.tags = initialData.tags
    stream.streamType = initialData.streamType
    stream.generator = generateGeneratorEntity(initialData)
    stream
  }

  /**
    * Generate generator entity from generator data
    *
    * @param streamInitialData - options for stream
    * @return - generated generator entity
    */
  def generateGeneratorEntity(streamInitialData: SjStreamData) = {
    var generator: Generator = null
    streamInitialData.streamType match {
      case "Tstream" =>
        generator = new Generator
        generator.generatorType = streamInitialData.generator.generatorType
        generator.generatorType match {
          case t: String if generatorTypesWithService.contains(t) =>
            var serviceName: String = null
            if (streamInitialData.generator.service contains "://") {
              val generatorUrl = new URI(streamInitialData.generator.service)
              if (generatorUrl.getScheme.equals("service-zk")) {
                serviceName = generatorUrl.getAuthority
              }
            } else {
              serviceName = streamInitialData.generator.service
            }
            generator.service = serviceDAO.get(serviceName)
          case _ =>
            generator.service = null
        }

        generator.instanceCount = streamInitialData.generator.instanceCount
      case _ =>
    }
    generator
  }

  /**
    * Validation of options for created module instance
    *
    * @param stream - stream
    * @return - list of errors
    */
  def validateStream(stream: SjStream, initialStreamData: SjStreamData) = {
    val validatorClassName = conf.getString("streams.validator-class")
    val validatorClass = Class.forName(validatorClassName)
    val validator = validatorClass.newInstance().asInstanceOf[StreamValidator]
    validator.validate(stream, initialStreamData)
  }

  /**
    * Save stream to db
    *
    * @param stream - stream entity
    * @return - name of saved entity
    */
  def saveStream(stream: SjStream) = {
    streamDAO.save(stream)
    stream.name
  }
}
