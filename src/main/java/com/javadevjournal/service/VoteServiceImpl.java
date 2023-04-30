package com.javadevjournal.service;

import com.javadevjournal.jpa.entity.Offer;
import com.javadevjournal.jpa.entity.Vote;
import com.javadevjournal.jpa.repository.VoteRepository;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

@AllArgsConstructor
@Service("voteService")
public class VoteServiceImpl implements VoteService {

	@Autowired
	private VoteRepository voteRepository;

	@Override
	public Vote createVote() {
		Vote vote = new Vote();
		return voteRepository.save(vote);
	}

	@Override
	public List<Vote> getAllOpenedVotes() {
		return voteRepository.findAllByOpened(true);
	}

	@Override
	public void save(Vote vote) {
		voteRepository.save(vote);
	}

	@Override
	public List<Vote> findAllByOfferListContains(Offer offer) {
		return voteRepository.findAllByOfferListContains(offer);
	}

	@Override
	public Optional<Vote> findById(Long id) {
		return voteRepository.findById(id);
	}

}