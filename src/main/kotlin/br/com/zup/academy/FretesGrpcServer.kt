package br.com.zup.academy

import com.google.protobuf.Any
import com.google.rpc.Code
import com.google.rpc.StatusProto
import io.grpc.Status
import io.grpc.protobuf.StatusProto.*
import io.grpc.stub.StreamObserver
import org.slf4j.LoggerFactory
import java.lang.IllegalStateException
import javax.inject.Singleton
import kotlin.random.Random

@Singleton
class FretesGrpcServer : FretesServiceGrpc.FretesServiceImplBase(){

    private val logger = LoggerFactory.getLogger(FretesGrpcServer::class.java)

    override fun calculaFrete(request: CalculaFreteRequest?, responseObserver: StreamObserver<CalculaFreteResponse>?) {

        logger.info("Calculando frete para request ${request}")

        //logica para validar se cep esta preenchido
        if(request!!.cep == null || request!!.cep.isBlank()){
            val e = Status.INVALID_ARGUMENT
                        .withDescription("cep deve ser informado")
                        .asRuntimeException()

            responseObserver?.onError(e)
        }

        //simulando erro com acesso negado
        if(request.cep.endsWith("333")){

            val acessoNegado = com.google.rpc.Status.newBuilder()
                        .setCode(Code.PERMISSION_DENIED.number)
                        .setMessage("usuario nao pode acessar este recurso")
                        .addDetails(Any.pack(ErroDetails.newBuilder()
                            .setCode(401)
                            .setMessage("Token expirado")
                            .build()))
                        .build()

           val e = toStatusRuntimeException(acessoNegado)//convertendo o erro para runtimeexception
            responseObserver?.onError(e)
        }

        //logica para verificar formato do cep
        if(!request.cep.matches("[0-9]{5}-[0-9]{3}".toRegex())){
            val e = Status.INVALID_ARGUMENT
                        .withDescription("cep no formato invalido")
                        .augmentDescription("forma esperado 00000-000")
                        .asRuntimeException()

            responseObserver?.onError(e)
        }

        var valor = 0.0

        //simulando erro com regra de negocio
        try {
            valor = Random.nextDouble(0.0,100.0)
            if (valor < 70){
                throw IllegalStateException("Erro executar regra de negocio")
            }
        }catch (e: Exception){
            responseObserver?.onError(Status.INTERNAL
                .withDescription(e.message)
                .withCause(e)
                .asRuntimeException())

        }

        val response = CalculaFreteResponse.newBuilder()
            .setCep(request!!.cep)
            .setValor(valor)
            .build()

        logger.info("Frete calculado ${response}")

        responseObserver!!.onNext(response)
        responseObserver.onCompleted()
    }
}