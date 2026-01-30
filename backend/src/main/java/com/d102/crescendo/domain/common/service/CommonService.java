package com.d102.crescendo.domain.common.service;

import com.d102.crescendo.domain.sheet.dto.response.GenreResponse;
import com.d102.crescendo.domain.sheet.dto.response.InstrumentResponse;
import com.d102.crescendo.domain.sheet.repository.GenreRepository;
import com.d102.crescendo.domain.sheet.repository.InstrumentRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
@RequiredArgsConstructor
public class CommonService {

    private final GenreRepository genreRepository;
    private final InstrumentRepository instrumentRepository;

    public List<GenreResponse> getGenres() {
        return genreRepository.findAll()
                .stream()
                .map(GenreResponse::from)
                .toList();
    }

    public List<InstrumentResponse> getInstruments() {
        return instrumentRepository.findAll()
                .stream()
                .map(InstrumentResponse::from)
                .toList();
    }
}