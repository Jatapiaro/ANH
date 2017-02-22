/*
 * To change this license header, choose License Headers in Project Properties.
 * To change this template file, choose Tools | Templates
 * and open the template in the editor.
 */
package com.ahm.fileupload;

import ahn.AHN;
import java.io.BufferedInputStream;
import java.io.BufferedOutputStream;
import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.Calendar;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.FilenameUtils;
import org.primefaces.model.UploadedFile;

/**
 *
 * @author jacobotapia
 */
@ViewScoped
@ManagedBean
public class FileController implements Serializable{


    private Part file;
    private final int limit_max_size=102400000;
    private final String limit_type_file = "csv";
    private final String path_to = "resources/files";
    private int moleculas = 2;
    private double eta = 0.0;
    
    
    public void algo() throws Exception{
        if(this.file!=null){
            String fname = this.getFileName(this.file);
            if (this.checkFileType(fname)) {
                String path = this.processUpload(file);

                String fileSavePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath(this.path_to);
                String pp = fileSavePath + "/" + path;
                try {
                    AHN.useAHN(pp, pp, this.moleculas, this.eta);
                } catch (Exception e) {
                    e.printStackTrace();
                }
                System.out.println("El archivo: " + pp);

                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletResponse response = (HttpServletResponse) context
                        .getExternalContext().getResponse();

                File filer = new File(pp);

                if (!filer.exists()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                String s = "resultados.csv";
                response.reset();
                response.setBufferSize(this.limit_max_size);
                response.setContentType("application/octet-stream");
                response.setHeader("Content-Length", String.valueOf(filer.length()));
                response.setHeader("Content-Disposition", "attachment;filename=\""
                        + filer.getName() + "\"");
                BufferedInputStream input = null;
                BufferedOutputStream output = null;
                try {
                    input = new BufferedInputStream(new FileInputStream(filer),
                            this.limit_max_size);
                    output = new BufferedOutputStream(response.getOutputStream(),
                            this.limit_max_size);
                    byte[] buffer = new byte[this.limit_max_size];
                    int length;
                    while ((length = input.read(buffer)) > 0) {
                        output.write(buffer, 0, length);
                    }
                } finally {
                    input.close();
                    output.close();
                }
                context.responseComplete();
                this.deleteFile(path);
            }else{
                FacesContext context = FacesContext.getCurrentInstance();
                FacesContext.getCurrentInstance().addMessage(
                        null, new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                "Error! Tu archivo no es un csv.",
                                "Tu archivo no es un csv."));
            }
        }
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

    private String getFileName(Part part) {
        for (String cd : part.getHeader("content-disposition").split(";")) {
            if (cd.trim().startsWith("filename")) {
                String filename = cd.substring(cd.indexOf('=') + 1).trim().replace("\"", "");
                return filename.substring(filename.lastIndexOf('/') + 1).substring(filename.lastIndexOf('\\') + 1);
            }
        }
        return null;
    }

    /**
     * @return the file
     */
    public Part getFile() {
        return file;
    }

    /**
     * @param file the file to set
     */
    public void setFile(Part file) {
        this.file = file;
    }
    
    

    public String processUpload(Part fileUpload) throws Exception {
        String fileSaveData = null;
        try {
            if (fileUpload.getSize() > 0) {

                String submittedFileName = this.getFileName(fileUpload);
                if (checkFileType(submittedFileName)) {

                    if (fileUpload.getSize() > this.limit_max_size) {
                        FacesContext.getCurrentInstance().addMessage(null, new FacesMessage(""));
                    } else {
                        String currentFileName = submittedFileName;
                        String extension = currentFileName.substring(currentFileName.lastIndexOf("."), currentFileName.length());
                        Long nameRandom = Calendar.getInstance().getTimeInMillis();

                        String newFileName = nameRandom + extension;

                        fileSaveData = newFileName;

                        String fileSavePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath(this.path_to);
                        System.out.println("Save path " + fileSavePath);
                        //System.out.println("Save path " + currentFileName+" ext: "+extension);

                        try {
                            byte[] fileContent = new byte[(int) fileUpload.getSize()];
                            InputStream in = fileUpload.getInputStream();
                            in.read(fileContent);
                            File fileToCreate = new File(fileSavePath, newFileName);
                            File folder = new File(fileSavePath);

                            if (!folder.exists()) {
                                folder.mkdir();
                            }

                            FileOutputStream fileOutStream = new FileOutputStream(fileToCreate);
                            fileOutStream.write(fileContent);
                            fileOutStream.flush();
                            fileOutStream.close();
                            fileSaveData = newFileName;
                        } catch (Exception e) {
                            fileSaveData = null;
                            throw e;
                        }
                    }
                } else {
                    fileSaveData = null;
                    System.out.println("No tiene formato valido");
                }
            } else {
                System.out.println("No tiene nada");
            }

        } catch (Exception e) {
            fileSaveData = null;
            throw e;
        }

        return fileSaveData;
    }
    
    public void deleteFile(String fileName) {

        String fileSavePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath(this.path_to);
        File fileToCreate = new File(fileSavePath + "/" + fileName);
        fileToCreate.delete();

    }

    /**
     * @return the moleculas
     */
    public int getMoleculas() {
        return moleculas;
    }

    /**
     * @param moleculas the moleculas to set
     */
    public void setMoleculas(int moleculas) {
        this.moleculas = moleculas;
    }

    /**
     * @return the eta
     */
    public double getEta() {
        return eta;
    }

    /**
     * @param eta the eta to set
     */
    public void setEta(double eta) {
        this.eta = eta;
    }
}
