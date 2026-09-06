package com.idipoedrine.shortlink.modules.url;

import com.idipoedrine.shortlink.common.exception.ResourceNotFoundException;
import com.idipoedrine.shortlink.modules.url.dto.CreateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.dto.ShortUrlResponse;
import com.idipoedrine.shortlink.modules.url.dto.UpdateShortUrlRequest;
import com.idipoedrine.shortlink.modules.url.mapper.ShortUrlMapper;
import com.idipoedrine.shortlink.modules.user.User;
import com.idipoedrine.shortlink.modules.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ShortUrlServiceTest {

    @Mock
    private ShortUrlRepository shortUrlRepository;
    @Mock
    private UserRepository userRepository;
    @Mock
    private ShortCodeGenerator shortCodeGenerator;
    @Mock
    private DestinationUrlValidator destinationUrlValidator;
    @Mock
    private ShortUrlMapper mapper;

    @InjectMocks
    private ShortUrlService shortUrlService;

    private final UUID ownerId = UUID.randomUUID();

    @Test
    void createsShortUrlForAuthenticatedOwner() {
        CreateShortUrlRequest request = new CreateShortUrlRequest("https://spring.io", null);
        when(shortCodeGenerator.generate()).thenReturn("abc1234");
        when(shortUrlRepository.existsByShortCode("abc1234")).thenReturn(false);
        when(userRepository.getReferenceById(ownerId)).thenReturn(new User());
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(ShortUrl.class))).thenReturn(
                new ShortUrlResponse(UUID.randomUUID(), "https://spring.io", "abc1234",
                        "http://localhost:8080/abc1234", true, null, null));

        ShortUrlResponse response = shortUrlService.create(ownerId, request);

        assertThat(response.shortCode()).isEqualTo("abc1234");
        verify(destinationUrlValidator).validate("https://spring.io");
    }

    @Test
    void retriesShortCodeGenerationOnCollision() {
        CreateShortUrlRequest request = new CreateShortUrlRequest("https://spring.io", null);
        when(shortCodeGenerator.generate()).thenReturn("dup0001", "fresh01");
        when(shortUrlRepository.existsByShortCode("dup0001")).thenReturn(true);
        when(shortUrlRepository.existsByShortCode("fresh01")).thenReturn(false);
        when(userRepository.getReferenceById(ownerId)).thenReturn(new User());
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(ShortUrl.class))).thenReturn(
                new ShortUrlResponse(UUID.randomUUID(), "https://spring.io", "fresh01",
                        "http://localhost:8080/fresh01", true, null, null));

        ShortUrlResponse response = shortUrlService.create(ownerId, request);

        assertThat(response.shortCode()).isEqualTo("fresh01");
        verify(shortCodeGenerator, times(2)).generate();
    }

    @Test
    void retriesOnceIfSaveFailsDueToARaceOnTheGeneratedCode() {
        CreateShortUrlRequest request = new CreateShortUrlRequest("https://spring.io", null);
        when(shortCodeGenerator.generate()).thenReturn("raced01", "safe002");
        when(shortUrlRepository.existsByShortCode(anyString())).thenReturn(false);
        when(userRepository.getReferenceById(ownerId)).thenReturn(new User());
        when(shortUrlRepository.saveAndFlush(any(ShortUrl.class)))
                .thenThrow(new DataIntegrityViolationException("duplicate key"))
                .thenAnswer(invocation -> invocation.getArgument(0));
        when(mapper.toResponse(any(ShortUrl.class))).thenReturn(
                new ShortUrlResponse(UUID.randomUUID(), "https://spring.io", "safe002",
                        "http://localhost:8080/safe002", true, null, null));

        ShortUrlResponse response = shortUrlService.create(ownerId, request);

        assertThat(response.shortCode()).isEqualTo("safe002");
        verify(shortUrlRepository, times(2)).saveAndFlush(any(ShortUrl.class));
    }

    @Test
    void rejectsAccessToAnotherUsersShortUrl() {
        UUID someoneElsesUrlId = UUID.randomUUID();
        when(shortUrlRepository.findByIdAndOwnerId(someoneElsesUrlId, ownerId)).thenReturn(Optional.empty());

        assertThatThrownBy(() -> shortUrlService.get(ownerId, someoneElsesUrlId))
                .isInstanceOf(ResourceNotFoundException.class);
    }

    @Test
    void updatesOnlyOwnedShortUrl() {
        UUID urlId = UUID.randomUUID();
        ShortUrl existing = ShortUrl.builder().id(urlId).active(true).build();
        when(shortUrlRepository.findByIdAndOwnerId(urlId, ownerId)).thenReturn(Optional.of(existing));
        when(shortUrlRepository.save(existing)).thenReturn(existing);
        when(mapper.toResponse(existing)).thenReturn(
                new ShortUrlResponse(urlId, null, null, null, false, null, null));

        ShortUrlResponse response = shortUrlService.update(ownerId, urlId, new UpdateShortUrlRequest(false, null));

        assertThat(existing.isActive()).isFalse();
        assertThat(response.active()).isFalse();
    }
}