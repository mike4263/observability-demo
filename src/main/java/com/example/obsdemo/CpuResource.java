package com.example.obsdemo;

import com.example.obsdemo.service.CpuLoadService;
import jakarta.inject.Inject;
import jakarta.ws.rs.DefaultValue;
import jakarta.ws.rs.POST;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.QueryParam;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.Response;

@Path("/api/cpu")
@Produces(MediaType.APPLICATION_JSON)
public class CpuResource {

    @Inject
    CpuLoadService cpuLoadService;

    @POST
    @Path("/load")
    public Response startLoad(
            @QueryParam("threads") @DefaultValue("1") int threads,
            @QueryParam("durationSeconds") @DefaultValue("10") int durationSeconds) {

        CpuLoadService.CpuLoadResult result = cpuLoadService.startLoad(threads, durationSeconds);

        if (!result.accepted()) {
            return Response.status(Response.Status.CONFLICT).entity(result.toMap()).build();
        }

        return Response.accepted(result.toMap()).build();
    }
}
