package com.code4ro.legalconsultation.service;

import com.code4ro.legalconsultation.converters.DocumentConsolidatedMapper;
import com.code4ro.legalconsultation.model.persistence.DocumentConfiguration;
import com.code4ro.legalconsultation.model.persistence.DocumentConsolidated;
import com.code4ro.legalconsultation.model.persistence.DocumentMetadata;
import com.code4ro.legalconsultation.model.persistence.DocumentNode;
import com.code4ro.legalconsultation.repository.DocumentConsolidatedRepository;
import com.code4ro.legalconsultation.service.api.CommentService;
import com.code4ro.legalconsultation.service.impl.DocumentConsolidatedService;
import com.code4ro.legalconsultation.factory.DocumentNodeFactory;
import com.code4ro.legalconsultation.factory.RandomObjectFiller;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.MockitoJUnitRunner;

import java.util.Optional;
import java.util.UUID;

import static org.junit.Assert.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.class)
public class DocumentConsolidatedServiceTest {

    @Mock
    private DocumentConsolidatedRepository documentConsolidatedRepository;
    @Mock
    private DocumentConsolidatedMapper mapperService;
    @Mock
    private CommentService commentService;

    @InjectMocks
    private DocumentConsolidatedService documentConsolidatedService;

    private final DocumentNodeFactory documentNodeFactory = new DocumentNodeFactory();

    @Test
    public void getDocument() {
        //given
        final UUID uuid = UUID.randomUUID();

        final UUID documentNodeUuid = UUID.randomUUID();
        String documentNodeTitle = "mockTitle";
        DocumentNode documentNode = new DocumentNode();
        documentNode.setId(documentNodeUuid);
        documentNode.setTitle(documentNodeTitle);

        String documentMetadataTitle = "documentMetadataTitle";
        DocumentMetadata documentMetadata = new DocumentMetadata();
        documentMetadata.setDocumentTitle(documentMetadataTitle);

        DocumentConfiguration documentConfiguration = new DocumentConfiguration();
        documentConfiguration.setOpenForCommenting(true);

        DocumentConsolidated documentConsolidated = new DocumentConsolidated();
        documentConsolidated.setDocumentMetadata(documentMetadata);
        documentConsolidated.setDocumentNode(documentNode);
        documentConsolidated.setDocumentConfiguration(documentConfiguration);


        when(documentConsolidatedRepository.findByDocumentMetadataId(any(UUID.class))).thenReturn(Optional.of(documentConsolidated));

        //when
        DocumentConsolidated document = documentConsolidatedService.getByDocumentMetadataId(uuid);

        //then
        assertEquals("DocumentNode id is different", documentNodeUuid, document.getDocumentNode().getId());
        assertEquals("DocumentNode title is different", documentNodeTitle, document.getDocumentNode().getTitle());

        assertEquals("DocumentMetadata title is different", documentMetadataTitle, document.getDocumentMetadata().getDocumentTitle());

        assertEquals("OpenForCommenting should be true", true, document.getDocumentConfiguration().getOpenForCommenting());
    }

    @Test
    public void getAllDocuments() {
        documentConsolidatedService.findAll();
        verify(documentConsolidatedRepository).findAll();
    }

    @Test
    public void saveDocument() {
        final DocumentMetadata documentMetadata = RandomObjectFiller.createAndFill(DocumentMetadata.class);
        DocumentConsolidated documentConsolidated =
                new DocumentConsolidated(documentMetadata, documentNodeFactory.create());
        documentConsolidatedService.saveOne(documentConsolidated);

        verify(documentConsolidatedRepository).save(documentConsolidated);
    }

    @Test
    public void deleteDocument() {
        final UUID id = UUID.randomUUID();
        documentConsolidatedService.deleteById(id);

        verify(documentConsolidatedRepository).deleteById(id);
    }
}
