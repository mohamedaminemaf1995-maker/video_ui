package com.local.ar44.service;

import com.local.ar44.dto.Video;
import com.local.ar44.repo.VideoRepository;
import org.springframework.stereotype.Service;
import org.w3c.dom.Document;
import org.w3c.dom.NodeList;
import org.xml.sax.InputSource;

import org.w3c.dom.Element;
import javax.xml.parsers.DocumentBuilder;
import javax.xml.parsers.DocumentBuilderFactory;
import java.io.StringReader;
import java.time.LocalDateTime;

@Service
public class XspfImportService {

    private final VideoRepository videoRepository;

    public XspfImportService(VideoRepository videoRepository) {
        this.videoRepository = videoRepository;
    }

    public void importFromXml(String xmlContent) throws Exception {
        DocumentBuilderFactory factory = DocumentBuilderFactory.newInstance();
        factory.setNamespaceAware(false);

        DocumentBuilder builder = factory.newDocumentBuilder();
        Document document = builder.parse(new InputSource(new StringReader(xmlContent)));

        NodeList tracks = document.getElementsByTagName("track");

        for (int i = 0; i < tracks.getLength(); i++) {
            Element track = (Element) tracks.item(i);

            String location = getTagValue(track, "location");
            String title = getTagValue(track, "title");
            String creator = getTagValue(track, "creator");
            String album = getTagValue(track, "album");
            String durationStr = getTagValue(track, "duration");
            String vlcIdStr = getTagValue(track, "vlc:id");

            Video video = new Video();
            video.setUrl(location);
            video.setTitle(title != null ? title : extractFileName(location));
            video.setCreator(creator);
            video.setAlbum(album);
            video.setDurationMs(durationStr != null ? Long.parseLong(durationStr) : null);
            video.setSourceIndex(vlcIdStr != null ? Integer.parseInt(vlcIdStr) : null);
            video.setCreatedAt(LocalDateTime.now());

            videoRepository.save(video);
        }
    }

    private String getTagValue(Element parent, String tagName) {
        NodeList nodes = parent.getElementsByTagName(tagName);
        if (nodes.getLength() > 0) {
            return nodes.item(0).getTextContent();
        }
        return null;
    }

    private String extractFileName(String url) {
        if (url == null || url.isBlank()) return "unknown";
        int idx = url.lastIndexOf('/');
        return idx >= 0 ? url.substring(idx + 1) : url;
    }
}
