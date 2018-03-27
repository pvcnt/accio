---
layout: docs
weight: 35
title: Mobility datasets
---

While Accio does not provide for now an integrated datasets support, we provide here for convenience a list of well-known mobility datasets that have been used in numerous papers.
Several initiatives have been conducted to publicly provide datasets coming from real-life data collections, each of them being summarized in the table below.

| Dataset | Location | Time span | #users | #events |
|:--------|:---------|:----------|:-------|:--------|
| [Cabspotting](#cabspotting) | San Fransisco, USA | 1 month | 536 | 11 million |
| [Geolife](#geolife) | Beijing, China | 5.5 years | 178 | 25 million |
| [MDC](#mobile-data-challenge-mdc) | Geneva region, Switzerland | 3 years | 185 | 11 million |
| [T-Drive](#t-drive) | Beijing, China | 1 week | 10,357 | 15 million |
| [Brightkite](#brightkite) | World | 1.5 years | 58,228 | 4 million |
| [Gowalla](#gowalla) | World | 1.5 years | 196,591 | 6 million |
{: class="table table-striped"}

## Cabspotting
**Download [the dataset](http://crawdad.org/~crawdad/epfl/mobility/20090224/cab/) (free registration required).**

The Cabspotting dataset contains GPS traces of taxi cabs in San Francisco (USA), collected in May 2008.

## Geolife
**Download [the dataset](https://www.microsoft.com/en-us/download/details.aspx?id=52367) and [its user guide](https://www.microsoft.com/en-us/research/publication/geolife-gps-trajectory-dataset-user-guide/).**

The Geolife dataset gathers GPS trajectories collected from April 2007 to August 2012 in Beijing (China).
The large majority of traces were collected with a high sampling rate, around 1 events every 1~5 seconds.
It was collected by Microsoft Research.

## Mobile Data Challenge (MDC)
**Register to [get access to the dataset](https://www.idiap.ch/dataset/mdc/download) (universities and non-profits are eligible).**

The MDC dataset involves 182 volunteers equipped with smartphones running a data collection software in the Lake Geneva region (Switzerland), collected between 2099 and 2011.
A privacy protection scheme based on k-anonymity has been performed on the raw data before releasing the MDC dataset.
This privacy preserving operation includes many manual operations which have obviously an impact on the outcome of LPPMs, but these impacts are difficult to fully understand.
It includes not only locations coming from the GPS sensor, but also data from various other sensors (e.g., accelerometer, battery).

## T-Drive
**Download a sample of the dataset ([part 6](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/06.zip), [part 7](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/07.zip), [part 8](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/08.zip), [part 9](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/09.zip), [part 10](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/010.zip), [part 11](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/011.zip), [part 12](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/012.zip), [part 13](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/013.zip), [part 14](https://www.microsoft.com/en-us/research/wp-content/uploads/2016/02/014.zip)) and [its user guide](https://www.microsoft.com/en-us/research/publication/t-drive-trajectory-data-sample/).**

T-Drive is another dataset collected in Beijing and featuring taxi drivers.
It features a high number of users (more than 10,000) over a very short period of time (one week).
Only a sample of the whole dataset, that was collected by Microsoft Research, is released.

## Brightkite
**Download [the dataset](https://snap.stanford.edu/data/loc-brightkite.html).**

Brightkite is a dataset exposing "check-ins" leaved by users of the social network of the same name.
Such a dataset is sparser than whole mobility datasets, because we only have places at which users deliberately checked in.
But in addition to check-in locations, it also comes with friendship relationships between users.

## Gowalla
**Download [the dataset](https://snap.stanford.edu/data/loc-gowalla.html).**

Gowalla is a dataset exposing "check-ins" leaved by users of the social network of the same name.
Like Brightkite, in addition to check-in locations, it also comes with friendship relationships between users.
