package com.example.obsdemo;

import com.example.obsdemo.service.TraceGeneratorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/traces")
@Produces(MediaType.APPLICATION_JSON)
public class TraceResource {

    @Inject
    TraceGeneratorService traceGeneratorService;

    @POST
    @Path("/simple")
    public Response generateSimple(
            @QueryParam("name") @DefaultValue("demo-trace-event") String name) {
        return Response.ok(traceGeneratorService.generateSimple(name).toMap()).build();
    }

    @POST
    @Path("/nested")
    public Response generateNested() {
        return Response.ok(traceGeneratorService.generateNested().toMap()).build();
    }

    @POST
    @Path("/error")
    public Response generateError(
            @QueryParam("message") @DefaultValue("Simulated error for observability demo") String message) {
        return Response.ok(traceGeneratorService.generateError(message).toMap()).build();
    }

    @POST
    @Path("/slow")
    public Response generateSlow(@QueryParam("delayMs") @DefaultValue("500") int delayMs) {
        return Response.ok(traceGeneratorService.generateSlow(delayMs).toMap()).build();
    }

    @POST
    @Path("/burst")
    public Response generateBurst(@QueryParam("count") @DefaultValue("5") int count) {
        return Response.ok(traceGeneratorService.generateBurst(count).toMap()).build();
    }
}
