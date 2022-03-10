package com.redhat.service.bridge.manager.workers;

import java.time.ZonedDateTime;
import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.Captor;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import com.redhat.service.bridge.manager.dao.WorkDAO;
import com.redhat.service.bridge.manager.models.Processor;
import com.redhat.service.bridge.manager.models.Work;
import com.redhat.service.bridge.manager.workers.id.WorkerIdProvider;

import io.vertx.mutiny.core.eventbus.EventBus;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.atLeastOnce;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
public class WorkManagerImplTest {

    private static final WorkerIdProvider WORKER_ID_PROVIDER = new WorkerIdProvider();

    private static final String RESOURCE_ID = "123";

    @Mock
    WorkDAO workDAO;

    @Mock
    EventBus eventBus;

    @Mock
    Processor resource;

    @Captor
    ArgumentCaptor<Work> workArgumentCaptor;

    private WorkManagerImpl manager;

    @BeforeEach
    protected void setup() {
        this.manager = new WorkManagerImpl();
        this.manager.workDAO = this.workDAO;
        this.manager.eventBus = this.eventBus;
        this.manager.workerIdProvider = WORKER_ID_PROVIDER;
    }

    @Test
    void scheduleFiresEventForNewWork() {
        when(resource.getId()).thenReturn(RESOURCE_ID);
        when(workDAO.findByManagedResource(resource)).thenReturn(null);

        manager.schedule(resource);

        verify(workDAO).persist(workArgumentCaptor.capture());

        Work work = workArgumentCaptor.getValue();
        assertThat(work).isNotNull();
        assertThat(work.getType()).contains(Processor.class.getName());
        assertThat(work.getManagedResourceId()).isEqualTo(RESOURCE_ID);

        verify(eventBus).requestAndForget(anyString(), eq(work));
    }

    @Test
    void scheduleDoesNotFireEventForExistingWork() {
        Work existing = mock(Work.class);
        when(workDAO.findByManagedResource(resource)).thenReturn(existing);

        manager.schedule(resource);

        verify(eventBus, never()).requestAndForget(anyString(), any(Work.class));
    }

    @Test
    void existsForNewWork() {
        assertThat(manager.exists(new Work())).isFalse();
    }

    @Test
    void existsForExistingWork() {
        Work existing = mock(Work.class);

        when(workDAO.findById(anyString())).thenReturn(existing);

        assertThat(manager.exists(new Work())).isTrue();
    }

    @Test
    void recordAttemptIncreasesRetry() {
        Work work = new Work();
        work.setAttempts(0);

        when(workDAO.findById(anyString())).thenReturn(work);

        manager.recordAttempt(work);

        assertThat(work.getAttempts()).isEqualTo(1);

        //A bit of an overkill checking our mocked invocation was called... but it is important
        verify(workDAO, atLeastOnce()).findById(work.getId());
    }

    @Test
    void completeWhenExists() {
        Work work = new Work();
        work.setAttempts(0);

        when(workDAO.findById(anyString())).thenReturn(work);

        manager.complete(work);

        verify(workDAO).deleteById(work.getId());
    }

    @Test
    void completeWhenNotExists() {
        when(workDAO.findById(anyString())).thenReturn(null);

        manager.complete(new Work());

        verify(workDAO, never()).deleteById(anyString());
    }

    @Test
    void processWorkQueue() {
        Work work1 = new Work();
        work1.setType("Type1");
        Work work2 = new Work();
        work2.setType("Type2");

        when(workDAO.findByWorkerId(anyString())).thenReturn(List.of(work1, work2));

        manager.processWorkQueue();

        verify(eventBus).requestAndForget("Type1", work1);
        verify(eventBus).requestAndForget("Type2", work2);
    }

    @Test
    void reconnectDroppedWorkers() {
        manager.reconnectDroppedWorkers();

        verify(workDAO).reconnectDroppedWorkers(eq(WORKER_ID_PROVIDER.getWorkerId()), any(ZonedDateTime.class));
    }

}
