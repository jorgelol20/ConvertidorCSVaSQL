package org.example;

import javax.swing.*;
import java.io.*;
import java.text.MessageFormat;
import java.util.Arrays;

public class Main {
    public static void main(String[] args) throws IOException {
        JFileChooser fileCSVChooser = new JFileChooser();
        fileCSVChooser.setFileSelectionMode(JFileChooser.FILES_ONLY);
        fileCSVChooser.showOpenDialog(null);
        String archivo = String.valueOf(fileCSVChooser.getSelectedFile());
        if(archivo.contains(".csv")) {
            crearArchivos(fileCSVChooser);
        }else{
            JOptionPane.showMessageDialog(null, "El archivo no es correcto");
            System.exit(0);
        }
    }
    public static void crearArchivos(JFileChooser fileCSVChooser) throws IOException {
        File archivoCSVOriginal = new File(fileCSVChooser.getSelectedFile().getAbsolutePath());
        //Obtener nombre de la tabla desde el fichero
        String[] splitArchivo = archivoCSVOriginal.getName().split("\\.");
        String[] splitArchivo2 = splitArchivo[0].split("\\(");
        String[] splitArchivo3 = splitArchivo2[splitArchivo2.length - 1].split("\\)");
        String nombreTabla = splitArchivo3[0];
        //Seleccion del directorio de destino
        JFileChooser directoryChooser = new JFileChooser();
        directoryChooser.setFileSelectionMode(JFileChooser.DIRECTORIES_ONLY);
        directoryChooser.showOpenDialog(null);
        //Creacion de los archivos

        convertirCSVaTXT(/*File*/ archivoCSVOriginal, /*String*/ nombreTabla, /*JFileChooser*/ directoryChooser);
    }

    public static void convertirCSVaTXT(File archivoCSVOriginal, String nombreTabla, JFileChooser directoryChooser) throws IOException {
        File archivoTXTSinFormato = new File(directoryChooser.getSelectedFile().getAbsolutePath() + "\\" + nombreTabla + "SinFormato.txt");
        File archivoTXTFormato = new File(directoryChooser.getSelectedFile().getAbsolutePath() + "\\" + nombreTabla + ".txt");
        if (archivoTXTSinFormato.createNewFile() && archivoTXTFormato.createNewFile()) {
        }
        //Escribir en el TXT el contenido del CSV
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivoCSVOriginal), "ISO-8859-1"));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivoTXTSinFormato), "UTF-8"))) {
            String line;
            while ((line = br.readLine()) != null) {
                //Cambiamos las comas de los valores por un caracter en ASCII para que no de error al tratar los datos
                line = line.replace("'", "").replace(",", "▄");
                String formattedLine = line.replace("\u00a0", "").replace(";", ",");
                bw.write(formattedLine);
                bw.newLine();
            }
        } catch (IOException e) {
            System.err.println("Error al convertir de CSV a TXT: " + e.getMessage());
        }
        convertirTXTaSQL(/*File*/ archivoCSVOriginal, /*File*/ archivoTXTSinFormato, /*File*/ archivoTXTFormato, /*String*/ nombreTabla);
    }

    public static void convertirTXTaSQL (File archivoCSVOriginal, File archivoTXTSinFormato, File archivoTXTFormato, String tablaOriginal){
        int contadorLineas = 0;
        try (BufferedReader br = new BufferedReader(new FileReader(archivoTXTSinFormato))) {
            while (br.readLine() != null) {
                contadorLineas++;
            }
        } catch (IOException e) {
            System.err.println("Error al leer el archivo: " + e.getMessage());
        }
        //Escribir en el archivo que va a ser el SQL el contenido del TXT formateado.
        try (BufferedReader br = new BufferedReader(new InputStreamReader(new FileInputStream(archivoTXTSinFormato), "UTF-8"));
             BufferedWriter bw = new BufferedWriter(new OutputStreamWriter(new FileOutputStream(archivoTXTFormato), "UTF-8"))) {
            String line;
            int contador = 0;
            int largoLinea = 0;
            while ((line = br.readLine()) != null) {
                String[] splittedLine = line.split(",");
                String formattedLine;
                int largo = splittedLine.length;
                if (largoLinea == 0) {
                    largoLinea = splittedLine.length;
                }
                if (splittedLine.length < largoLinea) {
                    line = line.concat("NULL");
                    splittedLine = line.split(",");
                }
                for(int i = 0; i < largo; i++){
                    //Intena pasar el valor a una variable integer. En caso de no poder significará que tiene letras o es nulo
                    try{
                        int valor = Integer.parseInt(splittedLine[i]);
                    }catch (Exception e){
                        //En caso de ser nullo, se le indicará el valor "NULL" sin comillas. En caso de ser un String, se le pondrá entre comillas simples.
                        if(splittedLine[i].isEmpty() || splittedLine[i].equals(" ") || splittedLine[i].contains("NULL")) {
                            splittedLine[i] = "NULL";
                        }else{
                            splittedLine[i] = "'" + splittedLine[i] + "'";
                        }
                    }
                }
                //Remplazamos los "[" y "]" por nada.
                String splittedLine2 = Arrays.toString(splittedLine).replace("[", "").replace("]", "");
                if (contador == 0) {
                    formattedLine = MessageFormat.format("INSERT INTO {0}({1}) VALUES ", tablaOriginal, splittedLine2.replace("'",""));
                    //Cambiamos los caracteres de ASCII en "," para que el valor esté correcto
                }else if(contador != contadorLineas-1){
                    formattedLine = MessageFormat.format("({0}),", splittedLine2.replace("▄",","));
                }else {
                    formattedLine = MessageFormat.format("({0})", splittedLine2.replace("▄",","));
                }
                contador++;
                bw.write(formattedLine);
                bw.newLine();
            }
        }catch (IOException e) {
            System.err.println("Error al convertir de TXT a SQL: " + e.getMessage());
        }
        if (archivoTXTFormato.renameTo(new File(archivoTXTFormato.getAbsolutePath().replace(".txt",".sql")))){
            JOptionPane.showMessageDialog(null, "El archivo se ha creado correctamente");
        }
    }
}