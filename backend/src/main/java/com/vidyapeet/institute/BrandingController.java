package com.vidyapeet.institute;

import com.vidyapeet.common.exception.Exceptions;
import com.vidyapeet.institute.dto.BrandingResponse;
import com.vidyapeet.institute.repository.InstituteRepository;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public endpoint that lets the frontend load an institute's branding by slug
 * before the user logs in, so each portal renders with its own identity.
 */
@RestController
@RequestMapping("/api/branding")
public class BrandingController {

    private final InstituteRepository instituteRepository;

    public BrandingController(InstituteRepository instituteRepository) {
        this.instituteRepository = instituteRepository;
    }

    @GetMapping("/{slug}")
    public BrandingResponse getBranding(@PathVariable String slug) {
        Institute institute = instituteRepository.findBySlug(slug)
                .orElseThrow(() -> Exceptions.notFound("No institute found for slug '" + slug + "'."));
        return new BrandingResponse(
                institute.getName(),
                institute.getSlug(),
                institute.getLogoUrl(),
                institute.getPrimaryColor());
    }
}
