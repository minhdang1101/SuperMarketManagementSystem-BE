package fu.se.smms.service;

import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface FileStorageService {

    String storeFile(MultipartFile file, String subDirectory);

    List<String> storeFiles(List<MultipartFile> files, String subDirectory);

    void deleteFile(String filePath);
}
