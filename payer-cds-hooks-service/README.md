A Payer CDS Hooks Service
===============

Service that provides an appointment-book CDS Hook to search a Patient by subscriberId or Patient Demographics.
<br>The returned result from these matching steps are:
* Subscriber Found (200)
* Subscriber Not Found (404)
* Subscriber Not Unique (500)

Card content contains a link to a Smart App page to perform a data import.