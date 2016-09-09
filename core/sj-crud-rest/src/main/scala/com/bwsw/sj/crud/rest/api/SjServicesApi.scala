package com.bwsw.sj.crud.rest.api

import java.text.MessageFormat

import akka.http.scaladsl.server.{Directives, RequestContext}
import com.bwsw.sj.common.DAL.model._
import com.bwsw.sj.common.rest.entities._
import com.bwsw.sj.common.rest.entities.service.ServiceData
import com.bwsw.sj.crud.rest.utils.ConvertUtil.serviceToServiceData
import com.bwsw.sj.crud.rest.utils.{CompletionUtils, ServiceUtil}
import com.bwsw.sj.crud.rest.validator.SjCrudValidator
import com.bwsw.sj.crud.rest.validator.service.ServiceValidator

trait SjServicesApi extends Directives with SjCrudValidator with CompletionUtils {

  val servicesApi = {
    pathPrefix("services") {
      pathEndOrSingleSlash {
        post { (ctx: RequestContext) =>
          val data = serializer.deserialize[ServiceData](getEntityFromContext(ctx))

          val service = createService(data)
          val errors = ServiceValidator.validate(data, service)
          var response: RestResponse = BadRequestRestResponse(Map("message" ->
            MessageFormat.format(messages.getString("rest.services.service.cannot.create"), errors.mkString(";")))
          )

          if (errors.isEmpty) {
            ServiceUtil.prepareService(service)
            serviceDAO.save(service)
            response = CreatedRestResponse(Map("message" -> MessageFormat.format(
              messages.getString("rest.services.service.created"),
              service.name
            )))
          }

          ctx.complete(restResponseToHttpResponse(response))
        } ~
          get {
            val services = serviceDAO.getAll
            var response: RestResponse = NotFoundRestResponse(Map("message" -> messages.getString("rest.services.notfound")))
            if (services.nonEmpty) {
              val entity = Map("services" -> services.map(s => serviceToServiceData(s)))
              response = OkRestResponse(entity)
            }

            complete(restResponseToHttpResponse(response))
          }
      } ~
        pathPrefix(Segment) { (serviceName: String) =>
          pathEndOrSingleSlash {
            get {
              val service = serviceDAO.get(serviceName)
              var response: RestResponse = NotFoundRestResponse(Map("message" ->
                MessageFormat.format(messages.getString("rest.services.service.notfound"), serviceName))
              )
              service match {
                case Some(x) =>
                  val entity = Map("services" -> serviceToServiceData(x))
                  response = OkRestResponse(entity)
                case None =>
              }

              complete(restResponseToHttpResponse(response))
            } ~
              delete {
                var response: RestResponse = UnprocessableEntityRestResponse(Map("message" ->
                  MessageFormat.format(messages.getString("rest.services.service.cannot.delete"), serviceName)))
                val streams = getUsedStreams(serviceName)
                if (streams.isEmpty) {
                  val service = serviceDAO.get(serviceName)
                  service match {
                    case Some(x) =>
                      ServiceUtil.deleteService(x)
                      serviceDAO.delete(serviceName)
                      response = OkRestResponse(Map("message" ->
                        MessageFormat.format(messages.getString("rest.services.service.deleted"), serviceName))
                      )
                    case None =>
                      response = NotFoundRestResponse(Map("message" ->
                        MessageFormat.format(messages.getString("rest.services.service.notfound"), serviceName))
                      )
                  }
                }

                complete(restResponseToHttpResponse(response))
              }
          }
        }
    }
  }

  def createService(data: ServiceData) = {
    var service = new Service
    data.serviceType match {
      case "CassDB" => service = new CassandraService
      case "ESInd" => service = new ESService
      case "KfkQ" => service = new KafkaService
      case "TstrQ" => service = new TStreamService
      case "ZKCoord" => service = new ZKService
      case "ArspkDB" => service = new AerospikeService
      case "JDBC" => service = new JDBCService
    }

    service
  }

  def getUsedStreams(serviceName: String) = {
    streamDAO.getAll.filter(s => s.service.name.equals(serviceName))
  }
}
