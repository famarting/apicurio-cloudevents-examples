package io.famartin.schemaregistry;

import io.famartin.schemaregistry.beans.SchemaBytePayload;
import io.famartin.schemaregistry.beans.SchemaGroup;
import java.lang.Integer;
import java.lang.String;
import java.util.List;
import javax.ws.rs.Consumes;
import javax.ws.rs.DELETE;
import javax.ws.rs.GET;
import javax.ws.rs.POST;
import javax.ws.rs.PUT;
import javax.ws.rs.Path;
import javax.ws.rs.PathParam;
import javax.ws.rs.Produces;

/**
 * A JAX-RS interface.  An implementation of this interface must be provided.
 */
@Path("/schemagroups")
public interface SchemagroupsResource {
  /**
   * Get all schema groups in namespace.
   */
  @GET
  @Produces("application/json")
  List<String> getGroups();

  /**
   * Get schema group description in registry namespace.
   */
  @Path("/{group-id}")
  @GET
  @Produces("application/json")
  SchemaGroup getGroup(@PathParam("group-id") SchemaBytePayload groupId);

  /**
   * Create schema group with specified format in registry namespace.
   */
  @Path("/{group-id}")
  @PUT
  @Consumes("application/json")
  void createGroup(@PathParam("group-id") SchemaBytePayload groupId, SchemaGroup data);

  /**
   * Delete schema group in schema registry namespace.
   */
  @Path("/{group-id}")
  @DELETE
  void deleteGroup(@PathParam("group-id") SchemaBytePayload groupId);

  /**
   * Returns schema by group id.
   */
  @Path("/{group-id}/schemas")
  @GET
  @Produces("application/json")
  List<String> getSchemasByGroup(@PathParam("group-id") SchemaBytePayload groupId);

  /**
   * Deletes all schemas under specified group id.
   */
  @Path("/{group-id}/schemas")
  @DELETE
  void deleteSchemasByGroup(@PathParam("group-id") SchemaBytePayload groupId);

  /**
   * Get latest version of schema.
   */
  @Path("/{group-id}/schemas/{schema-id}")
  @GET
  void getLatestSchema(@PathParam("group-id") SchemaBytePayload groupId,
      @PathParam("schema-id") SchemaBytePayload schemaId);

  /**
   * Register schema. If schema of specified name does not exist in specified group, schema is created at version 1. If schema of specified name exists already in specified group, schema is created at latest version + 1. If schema with identical content already exists, existing schema's ID is returned. 
   *
   */
  @Path("/{group-id}/schemas/{schema-id}")
  @POST
  @Consumes("application/json;format=avro")
  void createSchema(@PathParam("group-id") SchemaBytePayload groupId,
      @PathParam("schema-id") SchemaBytePayload schemaId, SchemaBytePayload data);

  @Path("/{group-id}/schemas/{schema-id}")
  @DELETE
  void deleteSchema(@PathParam("group-id") SchemaBytePayload groupId,
      @PathParam("schema-id") SchemaBytePayload schemaId);

  /**
   * Get list of versions for specified schema
   */
  @Path("/{group-id}/schemas/{schema-id}/versions")
  @GET
  @Produces("application/json;format=avro")
  List<Integer> getSchemaVersions(@PathParam("group-id") SchemaBytePayload groupId,
      @PathParam("schema-id") SchemaBytePayload schemaId);

  @Path("/{group-id}/schemas/{schema-id}/versions/{version-number}")
  @GET
  void getSchemaVersion(@PathParam("group-id") SchemaBytePayload groupId,
      @PathParam("schema-id") SchemaBytePayload schemaId,
      @PathParam("version-number") Integer versionNumber);

  @Path("/{group-id}/schemas/{schema-id}/versions/{version-number}")
  @DELETE
  void deleteSchemaVersion(@PathParam("group-id") SchemaBytePayload groupId,
      @PathParam("schema-id") SchemaBytePayload schemaId,
      @PathParam("version-number") Integer versionNumber);
}
