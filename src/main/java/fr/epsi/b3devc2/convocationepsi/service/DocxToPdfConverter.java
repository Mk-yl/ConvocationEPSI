package fr.epsi.b3devc2.convocationepsi.service;


import lombok.extern.slf4j.Slf4j;
import org.docx4j.Docx4J;
import org.docx4j.convert.out.FOSettings;
import org.docx4j.fonts.IdentityPlusMapper;
import org.docx4j.fonts.Mapper;
import org.docx4j.fonts.PhysicalFonts;
import org.docx4j.openpackaging.packages.WordprocessingMLPackage;
import org.springframework.stereotype.Service;

import java.io.*;

@Service
@Slf4j
public class DocxToPdfConverter {

    public static byte[] convertDocxToPdf(byte[] docxBytes) throws Exception {
        try (InputStream docxInputStream = new ByteArrayInputStream(docxBytes);
             ByteArrayOutputStream pdfOutputStream = new ByteArrayOutputStream()) {
        //Convertir le fichier DOCX en PDF
            WordprocessingMLPackage wordMLPackage = WordprocessingMLPackage.load(docxInputStream);
            Docx4J.toPDF(wordMLPackage, pdfOutputStream);

            return pdfOutputStream.toByteArray();
        }
    }
}
