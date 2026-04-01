
package com.local.ar44.service;

import com.local.ar44.dto.Creator;
import com.local.ar44.repo.CreatorRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@Service
public class CreatorService {
    @Autowired
    private CreatorRepository creatorRepository;

    public List<Creator> findAll() {
        return creatorRepository.findAll();
    }

    public Optional<Creator> findById(Long id) {
        return creatorRepository.findById(id);
    }

    public Creator save(Creator creator) {
        return creatorRepository.save(creator);
    }

    public void deleteById(Long id) {
        creatorRepository.deleteById(id);
    }

    public Optional<Creator> findByName(String name) {
        return creatorRepository.findAll().stream().filter(c -> c.getName().equals(name)).findFirst();
    }
}
