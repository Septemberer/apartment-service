package com.javadevjournal.service.impl;

import com.javadevjournal.dto.ApartmentDTO;
import com.javadevjournal.jpa.entity.Apartment;
import com.javadevjournal.jpa.entity.Customer;
import com.javadevjournal.jpa.entity.Offer;
import com.javadevjournal.jpa.entity.Vote;
import com.javadevjournal.jpa.repository.ApartmentRepository;
import com.javadevjournal.jpa.repository.CustomerRepository;
import com.javadevjournal.security.MyResourceNotFoundException;
import com.javadevjournal.service.repo.ApartmentService;
import com.javadevjournal.service.repo.OfferService;
import com.javadevjournal.service.repo.VoteService;
import lombok.AllArgsConstructor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import javax.transaction.Transactional;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@AllArgsConstructor
@Service("apartmentService")
public class ApartmentServiceImpl implements ApartmentService {

	private final OfferService offerService;
	private final VoteService voteService;
	@Autowired
	private ApartmentRepository apartmentRepository;
	@Autowired
	private CustomerRepository customerRepository;

	@Override
	public List<Apartment> findApartmentsByFilter(Long minPrice, Long maxPrice, Integer floor, Integer rooms) {
		return apartmentRepository.findAllByPriceBetweenAndFloorAndRooms(minPrice, maxPrice, floor, rooms);
	}

	@Override
	public List<Apartment> findMyApartments(Customer customer) {
		return apartmentRepository.findAllByOwner(customer);
	}

	@Override
	public List<Apartment> findAllApprovedApartments() {
		return apartmentRepository.findAllByApprovedIsTrue();
	}

	@Override
	public List<Apartment> findAllUnapprovedApartments() {
		return apartmentRepository.findAllByApprovedIsFalse();
	}

	@Override
	public void deleteAllByOwner(Customer customer) {
		apartmentRepository.deleteAllByOwner(customer);
	}

	@Override
	@Transactional
	public Apartment createApartment(ApartmentDTO apartmentDTO, Customer customer) {
		Apartment apartment = new Apartment();
		apartment.setOwner(customer);
		apartment.setPrice(apartmentDTO.getPrice());
		apartment.setFloor(apartmentDTO.getFloor());
		apartment.setAddress(apartmentDTO.getAddress());
		apartment.setRooms(apartmentDTO.getRooms());
		if (customer.isProfessional()) {
			apartment.setApproved(true);
			customer.incPositive();
			customerRepository.save(customer);
		} else {
			apartment.setVote(voteService.createVote(apartment));
		}
		return apartmentRepository.save(apartment);
	}

	@Override
	public void unApprove(Customer customer) {
		List<Offer> offerList = offerService.findAllByCustomer(customer);
		for (Offer offer : offerList) {
			List<Vote> voteList = voteService.findAllByOfferListContains(offer);
			for (Vote vote : voteList) {
				List<Offer> list = vote.getOfferList();
				vote.setOfferList(
						list.stream().filter(x -> x.getCustomer() != customer).collect(Collectors.toList())
				);
				vote.setOpened(true);
				voteService.save(vote);
				Apartment apartment = getApartmentByVote(vote);
				apartment.setApproved(false);
				save(apartment);
			}
			offerService.delete(offer);
		}
	}

	private Apartment getApartmentByVote(Vote vote) {
		return apartmentRepository.findByVote(vote).orElseThrow(() -> new MyResourceNotFoundException("Некорректное состояние БД"));
	}

	@Override
	public void closeVote(Apartment apartment, Long price) {
		apartment.setPrice(price);
		apartment.setApproved(true);
		Customer customer = apartment.getOwner();
		customer.incPositive();
		customerRepository.save(customer);
		apartmentRepository.save(apartment);
	}

	@Override
	public void save(Apartment apartment) {
		apartmentRepository.save(apartment);
	}

	@Override
	public void addOfferInVote(Vote vote, Offer offer) {
		if (!vote.isOpened()) {
			throw new MyResourceNotFoundException("Голосование уже закончено!");
		}
		List<Offer> list = vote.getOfferList();
		list.add(offer);
		if (list.size() >= 5) {
			Long price = 0L;
			for (Offer offer_ : list) {
				price += offer_.getPrice();
			}
			price = price / list.size();
			closeVote(getApartmentByVote(vote), price);
			vote.setOpened(false);
		}
		vote.setOfferList(list);
		voteService.save(vote);
	}

	@Override
	@Transactional
	public void makeOffer(Long id, Customer customer, Long price) {
		Offer offer = new Offer();
		offer.setCustomer(customer);
		offer.setPrice(price);
		offerService.save(offer);
		Optional<Vote> voteOptional = voteService.findById(id);
		if (!voteOptional.isPresent()) {
			throw new MyResourceNotFoundException("Ошибка в указанном ID голосования");
		}
		Vote vote = voteOptional.get();
		addOfferInVote(vote, offer);
	}
}
