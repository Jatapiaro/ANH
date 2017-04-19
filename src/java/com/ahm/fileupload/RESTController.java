package com.ahm.fileupload;

import ahn.AHN;
import com.sun.jersey.core.header.FormDataContentDisposition;
import com.sun.jersey.multipart.FormDataParam;
import java.io.BufferedWriter;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.text.NumberFormat;
import java.util.Calendar;
import javax.ws.rs.*;
import javax.faces.context.FacesContext;
import javax.servlet.ServletContext;
import javax.ws.rs.core.*;
import javax.ws.rs.core.Response.ResponseBuilder;

@Path("/file")
public class RESTController {
    
    @Context 
    ServletContext context;
    
    private final int limit_max_size = 102400000;
    private final String limit_type_file = "csv";
    private final String path_to = "resources/files";
    
    
    @Path("/results")
    @Produces("text/csv")
    @GET
    public Response getFile(@QueryParam("code") String code) {

        String direction = context.getRealPath(path_to)+"/"+code+".csv";
        File file = new File(direction);

        ResponseBuilder response = Response.ok((Object) file);
        response.header("Content-Disposition",
                "attachment; filename="+code+".csv");
        return response.build();
    }
    
    @Path("/delete")
    @GET
    public Response deleteFile(@QueryParam("code") String code) {
        String direction = context.getRealPath(path_to) + "/" + code + ".csv";
        File file = new File(direction);
        this.deleteFileMethod(direction);
        return Response.status(200).entity("OK\n").build(); 
    }
    
    private void deleteFileMethod(String fileName) {
        File fileToCreate = new File(fileName);
        fileToCreate.delete();
    }
    
    
    
    @Path("/upload")
    @Consumes(MediaType.MULTIPART_FORM_DATA)
    @POST
    public Response uploadFile(
            @FormDataParam("file") InputStream uploadedInputStream,
            @FormDataParam("file") FormDataContentDisposition fileDetail,
            @FormDataParam("molecules") int molecules,
            @FormDataParam("eta") double eta) {
        
        String output = "";
        if(uploadedInputStream!=null && fileDetail!=null){
            if (this.checkFileType(fileDetail.getFileName())) {
                String[] data = this.writeToFile(uploadedInputStream, fileDetail.getFileName());
                AHN.useAHN(data[0], data[0], molecules, eta);
                output = "Your download code is: " + data[1].split("\\.")[0]+"\n";
            } else {
                output = "Invalid file type\n";
            }
        }else{
            output = "Please send a csv file\n";
        }
        return Response.status(200).entity(output).build(); 
    }
    
    private String[] writeToFile(InputStream uploadedInputStream,String submittedFileName) {
        
        String currentFileName = submittedFileName;
        String extension = currentFileName.substring(currentFileName.lastIndexOf("."), currentFileName.length());
        Long nameRandom = Calendar.getInstance().getTimeInMillis();
        
        String newFileName = nameRandom + extension;
        String fileSavePath = context.getRealPath(path_to)+"/"+newFileName;
        
        try {
            OutputStream out = new FileOutputStream(new File(fileSavePath));
            int read = 0;
            byte[] bytes = new byte[1024];

            out = new FileOutputStream(new File(fileSavePath));
            while ((read = uploadedInputStream.read(bytes)) != -1) {
                out.write(bytes, 0, read);
            }
            out.flush();
            out.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        String[] arr = {fileSavePath,newFileName};
        return arr;
    }
    
    private boolean checkFileType(String fileName) {
        if (fileName.length() > 0) {
            String[] parts = fileName.split("\\.");
            if (parts.length > 0) {
                String extension = parts[parts.length - 1];
                return this.limit_type_file.contains(extension);
            }
        }
        return false;
    }


}