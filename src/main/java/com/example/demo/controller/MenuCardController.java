package com.example.demo.controller;

import com.dropbox.core.DbxException;
import com.dropbox.core.DbxRequestConfig;
import com.dropbox.core.v2.DbxClientV2;
import com.dropbox.core.v2.files.FileMetadata;
import com.dropbox.core.v2.files.WriteMode;
import com.dropbox.core.v2.sharing.CreateSharedLinkWithSettingsErrorException;
import com.dropbox.core.v2.sharing.ListSharedLinksResult;
import com.dropbox.core.v2.sharing.SharedLinkMetadata;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.nio.file.FileSystems;
import java.nio.file.Path;

@RestController
@CrossOrigin
@Slf4j
@RequestMapping("/menu/qr")
public class MenuCardController {

    private static final String ACCESS_TOKEN = "sl.BBP85AxjPqvW5TpV6iI2j5rFISoe1QbNGmi60TRCujP8jpUuhlg1pRkwfXdA_HQvItnxESTHqzy1pqGw76CMPOl1A0xrddtdaKl-jJ-HFCcunG79VQizYOp2YQx2SxGPvA5sN9OCP2kq";

    @GetMapping(value="/generate/{menuId}", produces = MediaType.IMAGE_PNG_VALUE)
    public byte[] generateQRCodeImage(@PathVariable String menuId, @RequestPart("file") MultipartFile file) throws Exception {

        // Create Dropbox client
        DbxRequestConfig config = DbxRequestConfig.newBuilder("dropbox/java-tutorial").build();
        DbxClientV2 client = new DbxClientV2(config, ACCESS_TOKEN);

        String qrImagePath = "/app/" + file.getOriginalFilename();
        String imageLinkUrl = "default";
        String uploadURL = "/QR/" + file.getOriginalFilename();

        log.info(imageLinkUrl);
        try (InputStream in = (file.getInputStream())) {
            FileMetadata metadata = client.files().uploadBuilder(uploadURL).withMode(WriteMode.OVERWRITE)
                    .uploadAndFinish(in);
            log.info(String.valueOf(metadata));
            ListSharedLinksResult result = client.sharing().listSharedLinksBuilder().withPath(uploadURL).withDirectOnly(true).start();
            imageLinkUrl = result.getLinks().toString();
            if(result.getLinks().size() < 1) {
                SharedLinkMetadata sharedLinkMetadata = client.sharing().createSharedLinkWithSettings(uploadURL);
                imageLinkUrl = sharedLinkMetadata.getUrl();
            }
        }
        catch (IOException e) {
            e.printStackTrace();
        }
        catch (CreateSharedLinkWithSettingsErrorException ex) {
            System.out.println(ex);
        }
        catch (DbxException ex) {
            System.out.println(ex);
        }

        QRCodeWriter barcodeWriter = new QRCodeWriter();
        BitMatrix bitMatrix =
                barcodeWriter.encode(imageLinkUrl, BarcodeFormat.QR_CODE, 200, 200);

        Path path = FileSystems.getDefault().getPath(qrImagePath);
        System.out.println(path);
        MatrixToImageWriter.writeToPath(bitMatrix, "PNG", path);

        ByteArrayOutputStream pngOutputStream = new ByteArrayOutputStream();
        MatrixToImageWriter.writeToStream(bitMatrix, "PNG", pngOutputStream);
        byte[] pngData = pngOutputStream.toByteArray();
        return pngData;
    }



}
