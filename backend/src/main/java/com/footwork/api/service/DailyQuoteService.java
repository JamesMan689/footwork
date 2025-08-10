package com.footwork.api.service;

import com.footwork.api.entity.DailyQuote;
import com.footwork.api.entity.DailyQuoteResponse;
import com.footwork.api.repository.DailyQuoteRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Optional;

@Service
public class DailyQuoteService {
    
    @Autowired
    private DailyQuoteRepository dailyQuoteRepository;
    
    public DailyQuoteResponse getDailyQuote() {
        // Get a random quote from the database
        Optional<DailyQuote> randomQuote = dailyQuoteRepository.findRandomQuote();
        
        if (randomQuote.isPresent()) {
            return convertToResponse(randomQuote.get());
        }
        
        // If no quotes exist, return a default motivational quote
        return getDefaultQuote();
    }
    
    private DailyQuoteResponse convertToResponse(DailyQuote dailyQuote) {
        return new DailyQuoteResponse(
            dailyQuote.getId(),
            dailyQuote.getQuote(),
            dailyQuote.getAuthor(),
            dailyQuote.getCategory()
        );
    }
    
    private DailyQuoteResponse getDefaultQuote() {
        return new DailyQuoteResponse(
            1L,
            "Success is not final, failure is not fatal: it is the courage to continue that counts.",
            "Winston Churchill",
            "MOTIVATION"
        );
    }
} 