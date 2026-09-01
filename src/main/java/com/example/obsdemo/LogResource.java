package com.example.obsdemo;

import com.example.obsdemo.service.LogGeneratorService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/logs")
@Produces(MediaType.APPLICATION_JSON)
public class LogResource {

    @Inject
    LogGeneratorService logGeneratorService;

    @POST
    public Response generateLogs(
            @QueryParam("level") @DefaultValue("INFO") String level,
            @QueryParam("message") @DefaultValue("Demo log event from observability app") String message,
            @QueryParam("count") @DefaultValue("1") int count) {

        LogGeneratorService.LogResult result = logGeneratorService.generate(level, message, count);
        return Response.ok(result.toMap()).build();
    }

    @POST
    @Path("/burst")
    public Response generateBurst(@QueryParam("count") @DefaultValue("10") int count) {
        LogGeneratorService.LogResult result = logGeneratorService.generateBurst(count);
        return Response.ok(result.toMap()).build();
    }
}
