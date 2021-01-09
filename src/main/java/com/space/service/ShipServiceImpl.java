package com.space.service;

import com.space.exceptions.NotFoundExceptionSpace;
import com.space.exceptions.SpaceException;
import com.space.model.Ship;
import com.space.model.ShipType;
import com.space.repository.ShipRepository;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.Calendar;
import java.util.Date;
import java.util.List;

@Service
public class ShipServiceImpl implements ShipService{

    @Autowired
    private ShipRepository shipRepository;

    @Override
    public List<Ship> getAllShips(Specification<Ship> shipSpecification) {
        return shipRepository.findAll(shipSpecification);
    }

    @Override
    public Page<Ship> getAllShips(Specification<Ship> shipSpecification, Pageable sortedByName) {
        return shipRepository.findAll(shipSpecification,sortedByName);
    }

    @Override
    public Ship createShip(Ship ship) {

        if(ship.getName() == null
                || ship.getPlanet() == null
                || ship.getShipType() == null
                || ship.getProdDate() == null
                || ship.getSpeed() == null
                || ship.getCrewSize() == null)
        {throw new SpaceException();}

        checkShipParams(ship);

        if (ship.getUsed() == null)
            ship.setUsed(false);

        Double raiting = calculateRating(ship);
        ship.setRating(raiting);

        return shipRepository.saveAndFlush(ship);
    }

    private void checkShipParams(Ship ship) {
        if(ship.getProdDate() != null){
            Calendar calendar = Calendar.getInstance();
            calendar.setTime(ship.getProdDate());
            int year = calendar.get(Calendar.YEAR);
            if(year > 3019 || year<2800)
                throw  new SpaceException();

        }


        if( (ship.getName() != null &&(  ship.getName().length() >50 || ship.getName().length() < 1))
        || (ship.getPlanet() !=null && (ship.getPlanet().length() > 50  || ship.getPlanet().length() < 1))
        || (ship.getSpeed() != null && (ship.getSpeed()<0.01D || ship.getSpeed()>0.99D) )
        || (ship.getCrewSize() != null && (ship.getCrewSize() < 0 ||  ship.getCrewSize() > 9999) )
        )
        {
            throw new SpaceException();
        }
    }

    private Double calculateRating(Ship ship) {
        double k = ship.getUsed() ? 0.5 : 1.0;

        Calendar calendar = Calendar.getInstance();
        calendar.setTime(ship.getProdDate());
        int year = calendar.get(Calendar.YEAR);
        BigDecimal rating = new BigDecimal((80.0 * ship.getSpeed() * k)/(3019 - year +1));
        rating = rating.setScale(2, RoundingMode.HALF_UP);
        return rating.doubleValue();
    }

    @Override
    public Ship editShip(Long id, Ship ship) {
        checkShipParams(ship);

        if(!shipRepository.existsById(id))
        {
            throw  new NotFoundExceptionSpace();
        }

        Ship shipEdit = shipRepository.findById(id).get();

        if(ship.getName() != null  )
             shipEdit.setName(ship.getName());
        if(ship.getPlanet() !=null)
            shipEdit.setPlanet(ship.getPlanet());
        if(ship.getShipType() != null)
            shipEdit.setShipType(ship.getShipType());
        if(ship.getProdDate() != null)
            shipEdit.setProdDate(ship.getProdDate());
        if(ship.getUsed() != null)
            shipEdit.setUsed(ship.getUsed());
        if(ship.getSpeed() != null)
            shipEdit.setSpeed(ship.getSpeed());
        if(ship.getCrewSize() != null)
            shipEdit.setCrewSize(ship.getCrewSize());

        shipEdit.setRating(calculateRating(shipEdit));

        return shipRepository.saveAndFlush(shipEdit);
    }

    @Override
    public void deleteById(Long id) {
        if(!shipRepository.existsById(id))
            {
                throw new NotFoundExceptionSpace();
            }
        shipRepository.deleteById(id);
    }

    @Override
    public Ship getShip(Long id) {
        if(!shipRepository.existsById(id))
        {
            throw new NotFoundExceptionSpace();
        }
        return shipRepository.findById(id).get();
    }

    @Override
    public Long idValid(String id) {
        if(id == null || id.equals("") || id.trim().equals("0") )
            throw new SpaceException();
        try{
            Long idLong = Long.valueOf(id);
            return idLong;}
        catch (Exception e){
            throw new SpaceException();
        }
    }

    @Override
    public Specification<Ship> filterByName(String name) {
        return (root, query, cb) -> name == null ? null : cb.like(root.get("name"), "%" + name + "%");
    }

    @Override
    public Specification<Ship> filterByPlanet(String planet) {
        return (root, query, cb) -> planet == null ? null : cb.like(root.get("planet"), "%" + planet + "%");
    }

    @Override
    public Specification<Ship> filterByShipType(ShipType shipType) {
        return (root, query, cb) -> shipType == null ? null : cb.equal(root.get("shipType"), shipType);
    }

    @Override
    public Specification<Ship> filterByDate(Long after, Long before) {
        return (root, query, cb) -> {
            if (after == null && before == null)
                return null;
            if (after == null) {
                Date before1 = new Date(before);
                return cb.lessThanOrEqualTo(root.get("prodDate"), before1);
            }
            if (before == null) {
                Date after1 = new Date(after);
                return cb.greaterThanOrEqualTo(root.get("prodDate"), after1);
            }
            Date before1 = new Date(before);
            Date after1 = new Date(after);
            return cb.between(root.get("prodDate"), after1, before1);
        };
    }

    @Override
    public Specification<Ship> filterByUsage(Boolean isUsed) {
        return (root, query, cb) -> {
            if (isUsed == null)
                return null;
            if (isUsed)
                return cb.isTrue(root.get("isUsed"));
            else return cb.isFalse(root.get("isUsed"));
        };
    }

    @Override
    public Specification<Ship> filterBySpeed(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;
            if (min == null)
                return cb.lessThanOrEqualTo(root.get("speed"), max);
            if (max == null)
                return cb.greaterThanOrEqualTo(root.get("speed"), min);

            return cb.between(root.get("speed"), min, max);
        };
    }

    @Override
    public Specification<Ship> filterByCrewSize(Integer min, Integer max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;
            if (min == null)
                return cb.lessThanOrEqualTo(root.get("crewSize"), max);
            if (max == null)
                return cb.greaterThanOrEqualTo(root.get("crewSize"), min);

            return cb.between(root.get("crewSize"), min, max);
        };
    }

    @Override
    public Specification<Ship> filterByRating(Double min, Double max) {
        return (root, query, cb) -> {
            if (min == null && max == null)
                return null;
            if (min == null)
                return cb.lessThanOrEqualTo(root.get("rating"), max);
            if (max == null)
                return cb.greaterThanOrEqualTo(root.get("rating"), min);

            return cb.between(root.get("rating"), min, max);
        };
    }
}
