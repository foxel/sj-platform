package com.bwsw.sj.crud.rest.api

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model.module.Instance
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.stream.SjStreamData
import com.bwsw.sj.common.utils.EngineLiterals
import com.bwsw.sj.common.utils.EngineLiterals._
import com.bwsw.sj.crud.rest.utils.CompletionUtils
import com.bwsw.sj.crud.rest.validator.SjCrudValidator

import scala.collection.mutable
import com.bwsw.sj.common.utils.SjStreamUtils._

trait SjStreamsApi extends Directives with SjCrudValidator with CompletionUtils {

  val streamsApi = {
    pathPrefix("streams") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          checkContext(ctx)
          val streamData = serializer.deserialize[SjStreamData](getEntityFromContext(ctx))
          val errors = streamData.validate()
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            createMessage("rest.streams.stream.cannot.create", errors.mkString(";"))))

          if (errors.isEmpty) {
            streamData.create()
            streamDAO.save(streamData.asModelStream())
            response = CreatedRestResponse(Map("message" ->
              createMessage("rest.streams.stream.created", streamData.name)))
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val streams = streamDAO.getAll
            var response: RestResponse = NotFoundRestResponse(Map("message" -> getMessage("rest.streams.notfound")))
            if (streams.nonEmpty) {
              val entity = Map("streams" -> streams.map(s => s.asProtocolStream()))
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix(Segment) { (streamName: String) =>
          pathEndOrSingleSlash {
            get {
              val stream = streamDAO.get(streamName)
              var response: RestResponse = NotFoundRestResponse(Map("message" ->
                createMessage("rest.streams.stream.notfound", streamName))
              )
              stream match {
                case Some(x) =>
                  val entity = Map("streams" -> x.asProtocolStream())
                  response = OkRestResponse(entity)
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                  createMessage("rest.streams.stream.cannot.delete", streamName)))

                val instances = getUsedInstances(streamName)

                if (instances.isEmpty) {
                  val stream = streamDAO.get(streamName)
                  stream match {
                    case Some(x) =>
                      x.delete()
                      streamDAO.delete(streamName)
                      response = OkRestResponse(Map("message" ->
                        createMessage("rest.streams.stream.deleted", streamName))
                      )
                    case None =>
                      response = NotFoundRestResponse(Map("message" ->
                        createMessage("rest.streams.stream.notfound", streamName))
                      )
                  }
                }

                complete(restResponseToHttpResponse(response))
              }
          }
        }
    }
  }

  private def getUsedInstances(streamName: String): mutable.Buffer[Instance] = {
    instanceDAO.getAll.filter { (instance: Instance) =>
      if (!instance.moduleType.equals(inputStreamingType)) {
        instance.inputs.map(clearStreamFromMode).contains(streamName) || instance.outputs.contains(streamName)
      } else {
        instance.outputs.contains(streamName)
      }
    }
  }
}
