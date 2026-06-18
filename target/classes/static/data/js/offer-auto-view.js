// offer-auto-view.js - Safe version
document.addEventListener('DOMContentLoaded', function() {
    console.log('Offer auto-view script loaded');

    // Only auto-view if explicitly requested via URL param (not flash attributes)
    const urlParams = new URLSearchParams(window.location.search);
    const autoView = urlParams.get('autoView');
    const offerId = urlParams.get('offerId');

    if (autoView === 'true' && offerId) {
        // Small delay to let page settle
        setTimeout(() => {
            window.open('/dashboard/hr/view-offer/' + offerId, '_blank');
        }, 1000);
    }
});