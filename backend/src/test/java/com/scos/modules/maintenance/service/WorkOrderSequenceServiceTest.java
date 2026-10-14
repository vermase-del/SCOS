package com.scos.modules.maintenance.service;

import com.scos.modules.maintenance.repository.WorkOrderSequenceRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import java.time.Year;
import java.util.UUID;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class WorkOrderSequenceServiceTest {
    @Mock WorkOrderSequenceRepository repository;
    @InjectMocks WorkOrderSequenceService service;

    @Test void incrementsCommunityYearCounterAndFormatsTicketNumber() {
        UUID community=UUID.randomUUID();
        int year=Year.now(java.time.ZoneOffset.UTC).getValue();
        when(repository.current(community,year)).thenReturn(1L,2L);
        assertEquals("WO-"+year+"-0001",service.generateTicketNumber(community));
        assertEquals("WO-"+year+"-0002",service.generateTicketNumber(community));
        verify(repository,times(2)).increment(community,year);
    }
}
