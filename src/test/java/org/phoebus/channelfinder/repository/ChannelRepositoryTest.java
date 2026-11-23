package org.phoebus.channelfinder.repository;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Answers.RETURNS_DEEP_STUBS;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.when;

import co.elastic.clients.elasticsearch.ElasticsearchClient;
import co.elastic.clients.elasticsearch.core.BulkRequest;
import co.elastic.clients.elasticsearch.core.BulkResponse;
import co.elastic.clients.elasticsearch.core.SearchRequest;
import co.elastic.clients.elasticsearch.core.bulk.BulkResponseItem;
import co.elastic.clients.elasticsearch.core.bulk.OperationType;
import java.io.IOException;
import java.util.List;
import java.util.stream.IntStream;
import java.util.stream.Stream;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.phoebus.channelfinder.configuration.ElasticConfig;
import org.phoebus.channelfinder.configuration.LegacyApiProperties;
import org.phoebus.channelfinder.entity.Channel;
import org.phoebus.channelfinder.exceptions.BatchWriteException;
import org.springframework.test.util.ReflectionTestUtils;

@ExtendWith(MockitoExtension.class)
class ChannelRepositoryTest {

  private static final int CHUNK_SIZE = 10;

  @Mock private ElasticConfig elasticConfig;

  @Mock(answer = RETURNS_DEEP_STUBS)
  private ElasticsearchClient client;

  private ChannelRepository channelRepository;

  @BeforeEach
  void setUp() {
    channelRepository = new ChannelRepository(elasticConfig, client, new LegacyApiProperties());
    ReflectionTestUtils.setField(channelRepository, "chunkSize", CHUNK_SIZE);
    when(elasticConfig.getES_CHANNEL_INDEX()).thenReturn("channelfinder");
  }

  @Test
  void indexAll_oneChannelRefused_reportsEveryWrittenChannel() throws IOException {
    refuseTheChannelNamedRefused();
    List<Channel> firstChunk =
        IntStream.range(0, CHUNK_SIZE).mapToObj(i -> new Channel("ch" + i, "owner")).toList();
    Channel accepted = new Channel("accepted", "owner");
    List<Channel> channels =
        Stream.concat(firstChunk.stream(), Stream.of(new Channel("refused", "owner"), accepted))
            .toList();

    BatchWriteException e =
        assertThrows(BatchWriteException.class, () -> channelRepository.indexAll(channels));

    assertEquals("Failed to index channels [refused]", e.getMessage());
    assertEquals(Stream.concat(firstChunk.stream(), Stream.of(accepted)).toList(), e.getWritten());
  }

  @Test
  void saveAll_oneChannelRefused_reportsEveryWrittenChannel() throws IOException {
    refuseTheChannelNamedRefused();
    when(elasticConfig.getES_QUERY_SIZE()).thenReturn(CHUNK_SIZE);
    when(client.search(any(SearchRequest.class), eq(Channel.class)).hits().hits())
        .thenReturn(List.of());
    Channel accepted = new Channel("accepted", "owner");
    List<Channel> channels = List.of(new Channel("refused", "owner"), accepted);

    BatchWriteException e =
        assertThrows(BatchWriteException.class, () -> channelRepository.saveAll(channels));

    assertEquals("Failed to index channels [refused]", e.getMessage());
    assertEquals(List.of(accepted), e.getWritten());
  }

  /** Elasticsearch refuses the channel named "refused" and accepts the others. */
  private void refuseTheChannelNamedRefused() throws IOException {
    doAnswer(
            invocation -> {
              BulkRequest request = invocation.getArgument(0);
              List<BulkResponseItem> items =
                  request.operations().stream().map(op -> item(op.index().id())).toList();
              return BulkResponse.of(
                  r ->
                      r.errors(items.stream().anyMatch(i -> i.error() != null))
                          .took(1)
                          .items(items));
            })
        .when(client)
        .bulk(any(BulkRequest.class));
  }

  private static BulkResponseItem item(String id) {
    if (id.equals("refused")) {
      return BulkResponseItem.of(
          i ->
              i.operationType(OperationType.Index)
                  .index("channelfinder")
                  .id(id)
                  .status(400)
                  .error(err -> err.type("document_parsing_exception").reason("too long")));
    }
    return BulkResponseItem.of(
        i -> i.operationType(OperationType.Index).index("channelfinder").id(id).status(201));
  }
}
