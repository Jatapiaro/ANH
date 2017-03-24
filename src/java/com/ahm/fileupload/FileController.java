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
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.Serializable;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;
import javax.annotation.PostConstruct;
import javax.faces.application.FacesMessage;
import javax.faces.bean.ManagedBean;
import javax.faces.bean.SessionScoped;
import javax.faces.component.UIViewRoot;
import javax.faces.context.ExternalContext;
import javax.faces.context.FacesContext;
import javax.faces.view.ViewScoped;
import javax.servlet.ServletOutputStream;
import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;
import javax.servlet.http.Part;
import org.apache.commons.io.FilenameUtils;
import org.omnifaces.util.Faces;
import org.primefaces.context.RequestContext;
import org.primefaces.model.UploadedFile;
import org.primefaces.model.chart.Axis;
import org.primefaces.model.chart.AxisType;
import org.primefaces.model.chart.ChartSeries;
import org.primefaces.model.chart.LineChartModel;
import org.primefaces.model.chart.LineChartSeries;

/**
 *
 * @author jacobotapia
 */
@SessionScoped
@ManagedBean
public class FileController implements Serializable{


    private Part file;
    private final int limit_max_size=102400000;
    private final String limit_type_file = "csv";
    private final String path_to = "resources/files";
    private int moleculas = 2;
    private double eta = 0.0;
    private List<Float> targets,estimates;
    private boolean activo = false;
    private LineChartModel lineModel1;
    private float targetsMin,targetsMax,estimastesMin,estimatesMax;
    
    @PostConstruct
    public void init() {
        if(!isActivo()){
            this.estimates = new ArrayList<Float>();
            this.targets  = new ArrayList<Float>();
            lineModel1 = new LineChartModel();
        }else{ 
            createLineModels();
        }
    }
    
    private void createLineModels() {
        lineModel1 = initLinearModel();
        lineModel1.setTitle("Graph");
        lineModel1.setLegendPosition("e");
        Axis yAxis = lineModel1.getAxis(AxisType.Y);
        float min = (float)Math.min(targetsMin, this.estimastesMin);
        yAxis.setMin(min-1);
        yAxis.setLabel("Values");
        float max = (float)Math.max(this.estimatesMax, this.targetsMax);
        yAxis.setMax(max+1);
        Axis xAxis = lineModel1.getAxis(AxisType.X);
        xAxis.setMin(0);
        xAxis.setMax(this.estimates.size());
        xAxis.setLabel("Observations");
    }
    
    private LineChartModel initLinearModel() {
        LineChartModel model = new LineChartModel();

        ChartSeries series1 = new ChartSeries();
        series1.setLabel("Targets");
        ChartSeries series2 = new ChartSeries();
        series2.setLabel("Estimates");
        System.out.println(this.estimates+"\n"+this.targets);
        for(int i=0;i<this.estimates.size();i++){
            series1.set(i+1, this.targets.get(i));
            series2.set(i+1, this.estimates.get(i));
        }
        
        model.addSeries(series1);
        model.addSeries(series2);

        return model;
    }
    
    public void readLastInputColumn(File csvFile){
        String line = "";
        String cvsSplitBy = ",";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                String[] data = line.split(cvsSplitBy);
                //System.out.println(data[2]);
                float val = Float.parseFloat(data[2]);
                if(val<this.targetsMin){
                    this.targetsMin = val;
                }
                if(val>this.targetsMax){
                    this.targetsMax = val;
                }
                targets.add(val);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void readResult(File csvFile) {
        String line = "";
        BufferedReader br = null;
        try {
            br = new BufferedReader(new FileReader(csvFile));
            while ((line = br.readLine()) != null) {
                float val = Float.parseFloat(line);
                if(val<this.estimastesMin){
                    this.estimastesMin = val;
                }
                if(val>this.estimatesMax){
                    this.estimatesMax = val;
                }
                estimates.add(val);
            }
        } catch (FileNotFoundException e) {
            e.printStackTrace();
        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            if (br != null) {
                try {
                    br.close();
                } catch (IOException e) {
                    e.printStackTrace();
                }
            }
        }
    }
    
    public void algo() throws Exception{
        if(this.file!=null){
            String fname = this.getFileName(this.file);
            if (this.checkFileType(fname)) {
                String path = this.processUpload(file);
                String fileSavePath = FacesContext.getCurrentInstance().getExternalContext().getRealPath(this.path_to);
                String pp = fileSavePath + "/" + path;
                try {
                    this.targetsMax = Float.MIN_VALUE;
                    this.targetsMin = Float.MAX_VALUE;
                    this.estimates = new ArrayList<Float>();
                    this.targets = new ArrayList<Float>();
                    File filBefore = new File(pp);
                    this.readLastInputColumn(filBefore);
                    AHN.useAHN(pp, pp, this.moleculas, this.eta);
                    File filAfter = new File(pp);
                    this.estimatesMax = Float.MIN_VALUE;
                    this.estimastesMin = Float.MAX_VALUE;
                    this.readResult(filAfter);
                    this.createLineModels();
                    this.activo = true;
                    RequestContext.getCurrentInstance().update("main:graf");
                } catch (Exception e) {
                    e.printStackTrace();
                }
            

                FacesContext context = FacesContext.getCurrentInstance();
                HttpServletResponse response = (HttpServletResponse) context
                        .getExternalContext().getResponse();

                File filer = new File(pp);

                if (!filer.exists()) {
                    response.sendError(HttpServletResponse.SC_NOT_FOUND);
                    return;
                }
                Faces.sendFile(filer, true);
                this.deleteFile(path);
            }else{
                FacesContext context = FacesContext.getCurrentInstance();
                FacesContext.getCurrentInstance().addMessage(
                        null, new FacesMessage(
                                FacesMessage.SEVERITY_ERROR,
                                "Error! Your file is not a csv.",
                                "Your file is not a csv."));
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

    /**
     * @return the lineModel1
     */
    public LineChartModel getLineModel1() {
        return lineModel1;
    }

    /**
     * @param lineModel1 the lineModel1 to set
     */
    public void setLineModel1(LineChartModel lineModel1) {
        this.lineModel1 = lineModel1;
    }
    
    public void reload() throws IOException {
        String contextPath =  FacesContext.getCurrentInstance().getExternalContext().getApplicationContextPath();
        FacesContext.getCurrentInstance().getExternalContext().redirect(contextPath);
    }

    /**
     * @return the activo
     */
    public boolean isActivo() {
        return activo;
    }

    /**
     * @param activo the activo to set
     */
    public void setActivo(boolean activo) {
        this.activo = activo;
    }
}
