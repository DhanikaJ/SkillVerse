/*
 * SkillVerse static-demo seed data.
 * All people, courses and numbers below are fictional test data.
 * Dates are expressed as "days ago" and resolved when the demo state is first created.
 */
(function () {
    'use strict';

    // Deterministic pseudo-random so every fresh demo has the same history.
    var seedValue = 20260101;
    function rnd() {
        seedValue = (seedValue * 1664525 + 1013904223) % 4294967296;
        return seedValue / 4294967296;
    }

    function xmlEscape(s) {
        return String(s).replace(/&/g, '&amp;').replace(/</g, '&lt;').replace(/>/g, '&gt;');
    }

    function wrapTitle(title) {
        var words = title.split(' '), lines = [''];
        words.forEach(function (w) {
            var last = lines[lines.length - 1];
            if (last && (last + ' ' + w).length > 20 && lines.length < 2) {
                lines.push(w);
            } else {
                lines[lines.length - 1] = last ? last + ' ' + w : w;
            }
        });
        return lines;
    }

    function thumb(title, c1, c2, glyph) {
        var lines = wrapTitle(title);
        var titleSvg = lines.map(function (l, i) {
            return '<text x="80" y="' + (270 + i * 38) + '" font-family="Arial,Helvetica,sans-serif" font-size="32" font-weight="700" fill="#ffffff">'
                    + xmlEscape(l) + '</text>';
        }).join('');
        var svg = '<svg xmlns="http://www.w3.org/2000/svg" width="640" height="360" viewBox="0 0 640 360">'
                + '<defs><linearGradient id="g" x1="0" y1="0" x2="1" y2="1">'
                + '<stop offset="0" stop-color="' + c1 + '"/><stop offset="1" stop-color="' + c2 + '"/></linearGradient></defs>'
                + '<rect width="640" height="360" fill="url(#g)"/>'
                + '<circle cx="540" cy="70" r="120" fill="#ffffff" fill-opacity="0.08"/>'
                + '<circle cx="90" cy="320" r="150" fill="#ffffff" fill-opacity="0.07"/>'
                + '<text x="80" y="170" font-family="Arial,Helvetica,sans-serif" font-size="100" fill="#ffffff" fill-opacity="0.9">' + glyph + '</text>'
                + titleSvg
                + '</svg>';
        return 'data:image/svg+xml;charset=utf-8,' + encodeURIComponent(svg);
    }

    var VIDEO = 'assets/videos/blockchain.mp4'; // placeholder lesson video shipped with the repo

    var categories = [
        {id: 1, type: 'Web Development'},
        {id: 2, type: 'Data Science'},
        {id: 3, type: 'Design'},
        {id: 4, type: 'Business'},
        {id: 5, type: 'Languages'},
        {id: 6, type: 'Technology'}
    ];

    var cities = ['Colombo', 'Kandy', 'Galle', 'Jaffna', 'Negombo', 'Matara', 'Kurunegala', 'Anuradhapura']
            .map(function (n, i) {
                return {id: i + 1, name: n};
            });

    var genders = [{id: 1, name: 'Female'}, {id: 2, name: 'Male'}, {id: 3, name: 'Prefer not to say'}];

    // ---- Users -------------------------------------------------------------
    // Demo logins (public on purpose): see demo/README.md
    var users = [
        {id: 1, fname: 'Demo', lname: 'Learner', email: 'learner@skillverse.demo', password: 'Learner@123',
            cityId: 1, genderId: 3, daysAgo: 120},
        {id: 2, fname: 'Lakmal', lname: 'Demo', email: 'instructor@skillverse.demo', password: 'Teacher@123',
            cityId: 2, genderId: 3, daysAgo: 150},
        {id: 3, fname: 'Nadeesha', lname: 'Perera', email: 'nadeesha.perera@example.com', password: 'Seed@12345', cityId: 1, genderId: 1, daysAgo: 200},
        {id: 4, fname: 'Kasun', lname: 'Silva', email: 'kasun.silva@example.com', password: 'Seed@12345', cityId: 3, genderId: 2, daysAgo: 190},
        {id: 5, fname: 'Amaya', lname: 'Fernando', email: 'amaya.fernando@example.com', password: 'Seed@12345', cityId: 5, genderId: 1, daysAgo: 180},
        {id: 6, fname: 'Ruwan', lname: 'Jayasinghe', email: 'ruwan.jayasinghe@example.com', password: 'Seed@12345', cityId: 4, genderId: 2, daysAgo: 170}
    ];
    // Extra learners so the "new users" report has something to count.
    var firstNames = ['Dilani', 'Tharindu', 'Sachini', 'Nuwan', 'Ishara', 'Chamod', 'Pavithra', 'Lakshan', 'Hansika', 'Dinesh', 'Malsha', 'Yasiru'];
    for (var i = 0; i < firstNames.length; i++) {
        users.push({
            id: 7 + i, fname: firstNames[i], lname: 'Student', email: firstNames[i].toLowerCase() + '.s@example.com',
            password: 'Seed@12345', cityId: 1 + (i % 8), genderId: 1 + (i % 3), daysAgo: 4 + Math.floor(rnd() * 85)
        });
    }

    // ---- Courses -----------------------------------------------------------
    function lessons(names) {
        return names.map(function (n) {
            return {title: n[0], video: VIDEO, resources: n[1] || []};
        });
    }

    var courses = [
        {id: 1, title: 'Modern JavaScript from Scratch', categoryId: 1, difficulty: 'B', pricelevel: 'P', price: 4900, instructorId: 3, rating: 4.7, daysAgo: 110,
            description: 'Learn variables, functions, the DOM and async code by building small browser projects.',
            art: ['#2563eb', '#1e3a8a', '{ }'],
            lessons: lessons([['Setting up your tools', ['setup-checklist.pdf']], ['Variables, types and functions', ['cheat-sheet.pdf']], ['Working with the DOM', []], ['Fetching data with async/await', ['api-exercises.pdf']]])},
        {id: 2, title: 'Responsive Web Design with Bootstrap', categoryId: 1, difficulty: 'B', pricelevel: 'F', price: 0, instructorId: 2, rating: 4.5, daysAgo: 100,
            description: 'Build mobile-first pages with the Bootstrap grid, components and utilities. Free course.',
            art: ['#0ea5e9', '#075985', '&lt;/&gt;'],
            lessons: lessons([['The grid system', ['grid-reference.pdf']], ['Navbars, cards and forms', []], ['Making it responsive', ['layout-challenges.pdf']]])},
        {id: 3, title: 'Java Servlets & Hibernate in Practice', categoryId: 1, difficulty: 'I', pricelevel: 'P', price: 7900, instructorId: 2, rating: 4.8, daysAgo: 90,
            description: 'Build a layered Java web app: servlets, JSON endpoints, Hibernate entities and a MySQL schema.',
            art: ['#7c3aed', '#3b0764', 'JV'],
            lessons: lessons([['Project structure and Ant', ['project-layout.pdf']], ['Servlets and filters', ['filter-notes.pdf']], ['Mapping entities with Hibernate', ['entity-diagram.pdf']], ['Sessions and authentication', []], ['Taking payments with PayHere', ['payment-flow.pdf']]])},
        {id: 4, title: 'Python for Data Analysis', categoryId: 2, difficulty: 'B', pricelevel: 'P', price: 5900, instructorId: 4, rating: 4.6, daysAgo: 85,
            description: 'Clean, explore and visualise data with pandas and matplotlib using real-world style datasets.',
            art: ['#16a34a', '#14532d', 'Py'],
            lessons: lessons([['Python refresher', []], ['DataFrames with pandas', ['dataset.pdf']], ['Cleaning messy data', []], ['Charts that tell a story', ['chart-guide.pdf']]])},
        {id: 5, title: 'Machine Learning Essentials', categoryId: 2, difficulty: 'A', pricelevel: 'P', price: 9900, instructorId: 4, rating: 4.4, daysAgo: 70,
            description: 'Regression, classification and model evaluation, with the maths explained intuitively.',
            art: ['#ea580c', '#7c2d12', 'ML'],
            lessons: lessons([['What is machine learning?', []], ['Linear regression', ['notebook-1.pdf']], ['Classification and metrics', ['metrics-sheet.pdf']], ['Avoiding overfitting', []]])},
        {id: 6, title: 'UI/UX Design Fundamentals', categoryId: 3, difficulty: 'B', pricelevel: 'P', price: 4500, instructorId: 5, rating: 4.9, daysAgo: 65,
            description: 'Principles of layout, colour, typography and user research for people new to design.',
            art: ['#db2777', '#831843', 'Aa'],
            lessons: lessons([['Design thinking basics', []], ['Typography and colour', ['palette-guide.pdf']], ['Wireframes to prototypes', ['wireframe-kit.pdf']]])},
        {id: 7, title: 'Figma for Teams', categoryId: 3, difficulty: 'I', pricelevel: 'F', price: 0, instructorId: 5, rating: 4.3, daysAgo: 60,
            description: 'Components, auto-layout and shared libraries for collaborative design work. Free course.',
            art: ['#9333ea', '#4c1d95', 'Fg'],
            lessons: lessons([['Frames and auto-layout', []], ['Components and variants', ['component-cheatsheet.pdf']], ['Sharing libraries', []]])},
        {id: 8, title: 'Startup Finance 101', categoryId: 4, difficulty: 'B', pricelevel: 'P', price: 3900, instructorId: 6, rating: 4.2, daysAgo: 55,
            description: 'Budgets, cash flow and pricing explained for first-time founders.',
            art: ['#ca8a04', '#713f12', 'Rs'],
            lessons: lessons([['Reading a profit and loss', ['pnl-template.pdf']], ['Cash flow forecasting', []], ['Pricing your product', ['pricing-worksheet.pdf']]])},
        {id: 9, title: 'Business English for Professionals', categoryId: 5, difficulty: 'I', pricelevel: 'P', price: 3500, instructorId: 6, rating: 4.5, daysAgo: 50,
            description: 'Meetings, emails and presentations in confident, clear workplace English.',
            art: ['#0d9488', '#134e4a', 'En'],
            lessons: lessons([['Writing clear emails', ['email-phrases.pdf']], ['Running a meeting', []], ['Presenting your ideas', ['presentation-tips.pdf']]])},
        {id: 10, title: 'Blockchain Fundamentals', categoryId: 6, difficulty: 'B', pricelevel: 'P', price: 6500, instructorId: 3, rating: 4.6, daysAgo: 45,
            description: 'How blocks, hashes, consensus and smart contracts fit together, without the hype.',
            art: ['#334155', '#0f172a', '#'],
            lessons: lessons([['Blocks and hashes', ['glossary.pdf']], ['Consensus explained', []], ['Smart contracts overview', ['contract-examples.pdf']], ['Real-world use cases', []]])},
        {id: 11, title: 'Intro to Git and GitHub', categoryId: 6, difficulty: 'B', pricelevel: 'F', price: 0, instructorId: 2, rating: 4.8, daysAgo: 40,
            description: 'Commits, branches, pull requests and resolving conflicts. Free course.',
            art: ['#dc2626', '#7f1d1d', 'Git'],
            lessons: lessons([['Your first repository', ['git-commands.pdf']], ['Branching and merging', []], ['Pull requests and reviews', ['review-checklist.pdf']]])},
        {id: 12, title: 'Cloud Computing Basics', categoryId: 6, difficulty: 'I', pricelevel: 'P', price: 6900, instructorId: 4, rating: 4.1, daysAgo: 30,
            description: 'Compute, storage, networking and cost basics across the major cloud models.',
            art: ['#0284c7', '#0c4a6e', 'Cl'],
            lessons: lessons([['IaaS, PaaS and SaaS', []], ['Storage and databases', ['storage-comparison.pdf']], ['Networking and security basics', []], ['Controlling cost', ['cost-checklist.pdf']]])}
    ];
    courses.forEach(function (c) {
        c.thumbnail = thumb(c.title, c.art[0], c.art[1], c.art[2]);
        delete c.art;
    });

    // ---- Orders / enrollments ---------------------------------------------
    // Demo learner already owns three courses (two purchases, one free).
    var paidIds = courses.filter(function (c) {
        return c.price > 0;
    }).map(function (c) {
        return c.id;
    });
    var orders = [
        {id: 1, userId: 1, courseIds: [1], daysAgo: 38},
        {id: 2, userId: 1, courseIds: [6, 2], daysAgo: 12}
    ];
    var nextOrder = 3;
    // Historical orders from other students, spread over ~90 days, for the reports page.
    for (var d = 90; d >= 1; d -= 3) {
        var buyer = 7 + Math.floor(rnd() * firstNames.length);
        var picks = [paidIds[Math.floor(rnd() * paidIds.length)]];
        if (rnd() > 0.7) {
            var second = paidIds[Math.floor(rnd() * paidIds.length)];
            if (picks.indexOf(second) < 0) {
                picks.push(second);
            }
        }
        orders.push({id: nextOrder++, userId: buyer, courseIds: picks, daysAgo: d});
    }

    var enrollments = [
        {userId: 1, courseId: 1, progress: 65, daysAgo: 38},
        {userId: 1, courseId: 6, progress: 100, daysAgo: 12},
        {userId: 1, courseId: 2, progress: 20, daysAgo: 12}
    ];

    window.SV_SEED = {
        version: 1,
        videoPlaceholder: VIDEO,
        categories: categories,
        cities: cities,
        genders: genders,
        users: users,
        courses: courses,
        orders: orders,
        enrollments: enrollments
    };
})();
