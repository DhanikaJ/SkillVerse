/*
 * SkillVerse static demo: in-browser stand-in for the Java servlet backend.
 *
 * The real app talks to servlets through fetch() ("CourseList", "Cart", "SignIn", ...).
 * This script intercepts those calls and answers them from seed data kept in
 * localStorage, so the unmodified HTML/JS front end runs on any static host.
 * Nothing here talks to a server; no data leaves the visitor's browser.
 */
(function () {
    'use strict';

    var CFG = window.SV_DEMO_CONFIG || {};
    var SEED = window.SV_SEED;
    var KEY = 'sv_demo_state_v1';
    var DAY = 86400000;
    var page = (location.pathname.split('/').pop() || 'index.html');

    // ------------------------------------------------------------------ gate
    // Soft gate: lets the link work from the portfolio only. It is obscurity, not security,
    // because everything in this demo is public test data anyway.
    function gateAllows() {
        if (!CFG.gate) {
            return true;
        }
        try {
            if (sessionStorage.getItem('sv_gate_ok') === '1') {
                return true;
            }
        } catch (e) {
        }
        var ok = false;
        var params = new URLSearchParams(location.search);
        if (CFG.token && params.get('access') === CFG.token) {
            ok = true;
            params.delete('access');
            var q = params.toString();
            try {
                history.replaceState(null, '', location.pathname + (q ? '?' + q : '') + location.hash);
            } catch (e) {
            }
        }
        if (!ok && document.referrer) {
            try {
                var host = new URL(document.referrer).hostname;
                (CFG.allowedHosts || []).forEach(function (h) {
                    if (host === h || host.slice(-(h.length + 1)) === '.' + h) {
                        ok = true;
                    }
                });
            } catch (e) {
            }
        }
        if (ok) {
            try {
                sessionStorage.setItem('sv_gate_ok', '1');
            } catch (e) {
            }
        }
        return ok;
    }

    if (!gateAllows()) {
        window.__SV_BLOCKED = true;
        location.replace('demo-gate.html');
        return;
    }

    // ----------------------------------------------------------------- state
    function fmtDate(ms) {
        var d = new Date(ms);
        function p(n) {
            return n < 10 ? '0' + n : '' + n;
        }
        return d.getFullYear() + '-' + p(d.getMonth() + 1) + '-' + p(d.getDate());
    }

    function buildSeedState() {
        var now = Date.now();
        var S = {v: 1, next: {user: 100, course: 100, lesson: 1000, order: 1000, enroll: 100}, users: [], courses: [],
            lessons: [], orders: [], enrollments: [], carts: {}};
        SEED.users.forEach(function (u) {
            S.users.push({id: u.id, fname: u.fname, lname: u.lname, email: u.email, password: u.password,
                verified: true, cityId: u.cityId, genderId: u.genderId, photo: '', created_at: now - u.daysAgo * DAY});
        });
        var lessonId = 1;
        SEED.courses.forEach(function (c) {
            S.courses.push({id: c.id, title: c.title, description: c.description, categoryId: c.categoryId,
                difficulty: c.difficulty, pricelevel: c.pricelevel, price: c.price, instructorId: c.instructorId,
                thumbnail: c.thumbnail, rating: c.rating, status: 'Active', created_at: now - c.daysAgo * DAY});
            c.lessons.forEach(function (l, idx) {
                S.lessons.push({id: lessonId++, courseId: c.id, title: l.title, video: l.video,
                    resources: l.resources.map(function (r) {
                        return 'demo-resource:' + r;
                    }), order: idx});
            });
        });
        SEED.orders.forEach(function (o) {
            var items = o.courseIds.map(function (cid) {
                return {courseId: cid, price: byId(S.courses, cid).price};
            });
            S.orders.push({id: o.id, userId: o.userId, items: items,
                total: items.reduce(function (a, i) {
                    return a + i.price;
                }, 0), status: 'Success', created_at: now - o.daysAgo * DAY});
        });
        var eid = 1;
        SEED.enrollments.forEach(function (e) {
            S.enrollments.push({id: eid++, userId: e.userId, courseId: e.courseId, progress: e.progress,
                enrolled_at: now - e.daysAgo * DAY});
        });
        S.next.enroll = Math.max(S.next.enroll, eid);
        return S;
    }

    function byId(list, id) {
        id = Number(id);
        for (var i = 0; i < list.length; i++) {
            if (list[i].id === id) {
                return list[i];
            }
        }
        return null;
    }

    var S;
    try {
        S = JSON.parse(localStorage.getItem(KEY));
        if (!S || S.v !== 1) {
            S = null;
        }
    } catch (e) {
        S = null;
    }
    if (!S) {
        S = buildSeedState();
    }
    function save() {
        try {
            localStorage.setItem(KEY, JSON.stringify(S));
        } catch (e) {
            /* storage unavailable: demo still works for this page view */
        }
    }
    save();

    // "Session" (who is signed in). Kept in localStorage so it is shared between tabs like a cookie;
    // falls back to memory when storage is blocked.
    var memSession = {};
    function sget(k) {
        try {
            return localStorage.getItem(k);
        } catch (e) {
            return memSession[k] || null;
        }
    }
    function sset(k, v) {
        try {
            if (v === null) {
                localStorage.removeItem(k);
            } else {
                localStorage.setItem(k, v);
            }
        } catch (e) {
            memSession[k] = v;
        }
    }
    function currentUser() {
        var id = sget('sv_uid');
        return id ? byId(S.users, id) : null;
    }
    function cartKey() {
        var u = currentUser();
        return u ? 'u' + u.id : 'guest';
    }
    function cartIds() {
        return S.carts[cartKey()] || [];
    }

    // ----------------------------------------------------------- page guards
    var MEMBER_PAGES = ['home.html', 'instructor.html', 'my-courses.html', 'online-degrees.html', 'cart.html',
        'checkout.html', 'lesson.html', 'my-learning.html', 'profile.html', 'course-view.html'];
    var PUBLIC_ONLY = ['', 'index.html', 'publicCourses.html'];
    if (MEMBER_PAGES.indexOf(page) >= 0 && !currentUser()) {
        location.replace('index.html');
        return;
    }
    if (PUBLIC_ONLY.indexOf(page) >= 0 && currentUser()) {
        location.replace('home.html');
        return;
    }

    // ---------------------------------------------------------------- helpers
    var EMAIL_RE = /^[a-zA-Z0-9_!#$%&'*+\/=?`{|}~^.-]+@[a-zA-Z0-9.-]+$/;
    var PW_RE = /^(?=.*[a-z])(?=.*[A-Z])(?=.*\d)(?=.*[@#$%^&+=]).{8,}$/;
    var PW_MSG = 'Password must contain at least one uppercase letter, one lowercase letter, one number, '
            + 'one special character, and be at least eight characters long.';

    function fullName(u) {
        return (u.fname + ' ' + u.lname).trim();
    }
    function categoryName(id) {
        var c = byId(SEED.categories, id);
        return c ? c.type : 'Unknown';
    }
    function diffName(ch) {
        return ch === 'B' ? 'Beginner' : ch === 'I' ? 'Intermediate' : ch === 'A' ? 'Advanced' : '';
    }
    function instructorOf(c) {
        var u = byId(S.users, c.instructorId);
        return u ? fullName(u) : 'Unknown';
    }
    function lessonsOf(courseId) {
        return S.lessons.filter(function (l) {
            return l.courseId === Number(courseId);
        }).sort(function (a, b) {
            return a.order - b.order;
        });
    }
    function enrolled(userId, courseId) {
        return S.enrollments.some(function (e) {
            return e.userId === userId && e.courseId === Number(courseId);
        });
    }
    function cartItemJson(courseId, idx) {
        var c = byId(S.courses, courseId);
        if (!c) {
            return null;
        }
        var ins = byId(S.users, c.instructorId);
        return {id: idx + 1, course: {id: c.id, title: c.title, description: c.description, price: c.price,
                thumbnail: c.thumbnail, pricelevel: c.pricelevel, difficulty: c.difficulty,
                category: {id: c.categoryId, type: categoryName(c.categoryId)},
                user: ins ? {fname: ins.fname, lname: ins.lname} : undefined}};
    }
    function cartItems() {
        return cartIds().map(function (cid, i) {
            return cartItemJson(cid, i);
        }).filter(Boolean);
    }

    // PayHere sandbox: orders become paid when the fake checkout dialog is confirmed.
    function fulfilOrder(order) {
        order.status = 'Success';
        var key = 'u' + order.userId;
        order.items.forEach(function (it) {
            if (!enrolled(order.userId, it.courseId)) {
                S.enrollments.push({id: S.next.enroll++, userId: order.userId, courseId: it.courseId, progress: 0,
                    enrolled_at: Date.now()});
            }
        });
        S.carts[key] = [];
        save();
    }

    // ---------------------------------------------------------------- routes
    function ok(obj) {
        return {status: 200, body: obj};
    }
    function body(init) {
        if (!init || !init.body || typeof init.body !== 'string') {
            return {};
        }
        try {
            return JSON.parse(init.body);
        } catch (e) {
            return {};
        }
    }

    var routes = {
        CourseList: function () {
            return ok({status: true, courses: S.courses.map(function (c) {
                    return {id: c.id, title: c.title, img: c.thumbnail, description: c.description,
                        categoryId: c.categoryId, difficulty: c.difficulty, pricelevel: c.pricelevel,
                        instructor: instructorOf(c), university: categoryName(c.categoryId), rating: c.rating};
                })});
        },
        CategoryData: function () {
            return ok(SEED.categories);
        },
        CityData: function () {
            return ok(SEED.cities);
        },
        GenderData: function () {
            return ok(SEED.genders);
        },
        CourseDetails: function (m, url) {
            var c = byId(S.courses, url.searchParams.get('id'));
            if (!c) {
                return ok({status: false, message: 'Course not found'});
            }
            return ok({status: true, course: {id: c.id, title: c.title, img: c.thumbnail, description: c.description,
                    category: categoryName(c.categoryId), status: c.status, createdAt: new Date(c.created_at).toString(),
                    instructor: instructorOf(c), publisher: 'Skillverse Team', price: c.price,
                    lessons: lessonsOf(c.id).map(function (l) {
                        return l.title;
                    })}});
        },
        SignIn: function (m, url, init) {
            if (m === 'GET') {
                return ok({message: currentUser() ? '1' : '2'});
            }
            var b = body(init), email = (b.email || '').trim(), pw = b.password || '';
            var r = {status: false};
            if (!email) {
                r.message = 'Email cannot be empty!';
            } else if (!EMAIL_RE.test(email)) {
                r.message = 'Please enter a valid email!';
            } else if (!pw) {
                r.message = 'password cannot be empty';
            } else if (!PW_RE.test(pw)) {
                r.message = PW_MSG;
            } else {
                var u = S.users.filter(function (x) {
                    return x.email.toLowerCase() === email.toLowerCase() && x.password === pw;
                })[0];
                if (!u) {
                    r.message = 'Invalid Credentials';
                } else {
                    r.status = true;
                    if (!u.verified) {
                        sset('sv_pending', u.email);
                        r.message = '1';
                    } else {
                        sset('sv_uid', String(u.id));
                        r.message = '2';
                    }
                }
            }
            return ok(r);
        },
        SignUp: function (m, url, init) {
            var b = body(init), email = (b.email || '').trim(), r = {status: false};
            if (!email) {
                r.message = 'Email can not be empty!';
            } else if (!EMAIL_RE.test(email)) {
                r.message = 'Please enter a valid email!';
            } else if (!(b.firstName || '').trim()) {
                r.message = 'Name can not be empty!';
            } else if (!PW_RE.test(b.password || '')) {
                r.message = PW_MSG;
            } else if (S.users.some(function (x) {
                return x.email.toLowerCase() === email.toLowerCase();
            })) {
                r.message = 'User with this email already exists!';
            } else {
                S.users.push({id: S.next.user++, fname: b.firstName.trim(), lname: (b.lastName || '').trim(),
                    email: email, password: b.password, verified: false, cityId: 0, genderId: 0, photo: '',
                    created_at: Date.now()});
                save();
                sset('sv_pending', email);
                r.status = true;
                r.message = 'Demo mode: no email is sent. Enter the code 123456 to verify your account.';
            }
            return ok(r);
        },
        VerifyAccount: function (m, url, init) {
            var email = sget('sv_pending');
            if (!email) {
                return ok({status: false, message: '1'});
            }
            if ((body(init).verificationCode || '') !== '123456') {
                return ok({status: false, message: 'Invalid Verification Code! (demo code is 123456)'});
            }
            var u = S.users.filter(function (x) {
                return x.email.toLowerCase() === email.toLowerCase();
            })[0];
            if (u) {
                u.verified = true;
                save();
                sset('sv_uid', String(u.id)); // the demo signs you straight in
            }
            sset('sv_pending', null);
            return ok({status: true, message: 'Verification Successful!'});
        },
        SignOut: function () {
            var was = !!currentUser();
            sset('sv_uid', null);
            return ok({status: was});
        },
        UserProfile: function (m, url, init) {
            var u = currentUser();
            if (!u) {
                return ok({});
            }
            if (m === 'GET') {
                return ok({firstName: u.fname, lastName: u.lname, name: fullName(u), photo: u.photo || '',
                    cityId: u.cityId || 0, genderId: u.genderId || 0});
            }
            var b = body(init), r = {status: false}, name = (b.name || '').trim();
            var np = b.newpassword || '', cp = b.currentpassword || '', cf = b.confirmpassword || '';
            if (!name) {
                r.message = 'Your Name cannot be empty';
            } else if (Number(b.citySelect) === 0) {
                r.message = 'Select your City';
            } else if (Number(b.genderSelect) === 0) {
                r.message = 'Select your Gender';
            } else if (np && !cp) {
                r.message = 'Enter your current password';
            } else if (np && !PW_RE.test(np)) {
                r.message = 'The password must contains at least uppercase, lowercase, number, special character and to be minimum eight characters long!';
            } else if (np && np === cp) {
                r.message = 'The new password cannot match the current password';
            } else if (cf !== np) {
                r.message = 'Confirmed password does not matching entered new password!';
            } else if (np && cp !== u.password) {
                r.message = 'Current password is incorrect!';
            } else {
                var parts = name.split(/\s+/);
                u.fname = parts[0];
                u.lname = parts.slice(1).join(' ');
                u.cityId = Number(b.citySelect);
                u.genderId = Number(b.genderSelect);
                if (b.photo && b.photo.indexOf('data:') === 0) {
                    u.photo = b.photo;
                }
                if (np) {
                    u.password = np;
                }
                save();
                r.status = true;
                r.message = 'User profile details updated successfully!';
            }
            return ok(r);
        },
        InstructorProfile: function () {
            var u = currentUser();
            if (!u) {
                return ok({});
            }
            var d = new Date(u.created_at);
            var months = ['Jan', 'Feb', 'Mar', 'Apr', 'May', 'Jun', 'Jul', 'Aug', 'Sep', 'Oct', 'Nov', 'Dec'];
            return ok({firstName: u.fname, lastName: u.lname, email: u.email, photo: u.photo || '',
                since: months[d.getMonth()] + ' ' + d.getFullYear()});
        },
        CartCount: function () {
            return ok({status: true, count: cartIds().length});
        },
        Cart: function (m, url, init) {
            var key = cartKey();
            if (m === 'GET') {
                return ok({status: true, cartItems: cartItems()});
            }
            if (m === 'DELETE') {
                S.carts[key] = [];
                save();
                return ok({status: true, message: 'Cart cleared.'});
            }
            var cid = Number(body(init).courseId);
            var c = byId(S.courses, cid), u = currentUser();
            if (!cid || !c) {
                return ok({status: false, message: cid ? 'Course not found.' : 'Invalid course.'});
            }
            if (u && enrolled(u.id, cid)) {
                return ok({status: false, message: 'You already own this course.'});
            }
            var list = S.carts[key] || (S.carts[key] = []);
            if (list.indexOf(cid) >= 0) {
                return ok({status: false, message: 'Already in cart.'});
            }
            list.push(cid);
            save();
            return ok({status: true, message: 'Added to cart.'});
        },
        RemoveCartItem: function (m, url, init) {
            var key = cartKey(), cid = Number(body(init).courseId), list = S.carts[key] || [];
            var i = list.indexOf(cid);
            if (i < 0) {
                return ok({status: false, message: 'Course not found in cart.'});
            }
            list.splice(i, 1);
            S.carts[key] = list;
            save();
            return ok({status: true, message: 'Removed from cart.'});
        },
        CheckoutUserCart: function () {
            var u = currentUser();
            if (!u) {
                return ok({status: false, message: 'User not logged in.'});
            }
            return ok({status: true, user: {fullName: fullName(u), email: u.email}, cartItems: cartItems()});
        },
        CheckOut: function (m, url, init) {
            var u = currentUser(), b = body(init);
            if (!u) {
                return ok({status: false, message: 'Session expired. Please log in.'});
            }
            if (!(b.fullName || '').trim() || !(b.email || '').trim()) {
                return ok({status: false, message: 'Full name and email are required.'});
            }
            var ids = cartIds();
            if (!ids.length) {
                return ok({status: false, message: 'Your cart is empty.'});
            }
            var items = ids.map(function (cid) {
                return {courseId: cid, price: byId(S.courses, cid).price};
            });
            var total = items.reduce(function (a, i) {
                return a + i.price;
            }, 0);
            var order = {id: S.next.order++, userId: u.id, items: items, total: total, status: 'Pending',
                created_at: Date.now()};
            S.orders.push(order);
            save();
            var names = (b.fullName || '').trim().split(' ');
            return ok({status: true, payhereJson: {
                    sandbox: true, merchant_id: 'DEMO', return_url: location.origin + '/success.html',
                    cancel_url: location.origin + '/cancel.html', notify_url: location.origin + '/VerifyPayments',
                    order_id: '#000' + order.id,
                    items: items.map(function (i) {
                        return byId(S.courses, i.courseId).title;
                    }).join(', '),
                    amount: total.toFixed(2), currency: 'LKR', hash: '00000000000000000000000000000000',
                    first_name: names[0] || 'Demo', last_name: names[1] || 'User', email: b.email.trim(),
                    phone: '', address: '', city: '', country: 'Sri Lanka'}});
        },
        GetLatestOrder: function () {
            var u = currentUser();
            if (!u) {
                return ok({status: false, message: 'Session expired. Please log in.'});
            }
            var mine = S.orders.filter(function (o) {
                return o.userId === u.id && o.status === 'Success';
            }).sort(function (a, b) {
                return b.created_at - a.created_at;
            });
            if (!mine.length) {
                return ok({status: false, message: 'No orders found'});
            }
            return ok({status: true, order: {id: mine[0].id, total: mine[0].total, status: 'Success',
                    created_at: new Date(mine[0].created_at).toISOString()}});
        },
        MyPurchases: function () {
            var u = currentUser();
            if (!u) {
                return ok({status: false, message: 'Not logged in'});
            }
            var out = [];
            S.orders.filter(function (o) {
                return o.userId === u.id && o.status === 'Success';
            }).sort(function (a, b) {
                return b.created_at - a.created_at;
            }).forEach(function (o) {
                o.items.forEach(function (it) {
                    var c = byId(S.courses, it.courseId);
                    out.push({course_id: c.id, title: c.title, instructor: instructorOf(c),
                        difficulty: diffName(c.difficulty), thumbnail: c.thumbnail,
                        purchased_at: fmtDate(o.created_at), price: it.price});
                });
            });
            return ok({status: true, purchases: out});
        },
        MyLearning: function () {
            var u = currentUser();
            if (!u) {
                return ok({status: false, code: 401, message: 'Not signed in.'});
            }
            var inProgress = [], completed = [];
            S.enrollments.filter(function (e) {
                return e.userId === u.id;
            }).forEach(function (e) {
                var c = byId(S.courses, e.courseId);
                var item = {enrollment_id: e.id, course_id: c.id, title: c.title, instructor: instructorOf(c),
                    thumbnail: c.thumbnail, progress: Math.round(e.progress),
                    status: e.progress >= 100 ? 'Completed' : 'In-progress', enrolled_at: fmtDate(e.enrolled_at)};
                (e.progress >= 100 ? completed : inProgress).push(item);
            });
            return ok({status: true, in_progress: inProgress, completed: completed});
        },
        LessonData: function (m, url) {
            var u = currentUser();
            if (!u) {
                return ok({status: false, message: 'Please log in to view lessons.'});
            }
            var c = byId(S.courses, url.searchParams.get('course_id'));
            if (!c) {
                return ok({status: false, message: 'Course not found.'});
            }
            if (!enrolled(u.id, c.id) && c.instructorId !== u.id) {
                return ok({status: false, message: "You don't have access to this course."});
            }
            return ok({status: true, course: {id: c.id, title: c.title, thumbnail: c.thumbnail,
                    publisher_name: instructorOf(c)},
                lessons: lessonsOf(c.id).map(function (l) {
                    return {id: l.id, title: l.title, video_url: l.video, resource_files: l.resources};
                })});
        },
        MyCourseList: function () {
            var u = currentUser();
            if (!u) {
                return ok({status: false, message: 'User not logged in'});
            }
            var mine = S.courses.filter(function (c) {
                return c.instructorId === u.id;
            });
            return ok({status: true, totalCourses: mine.length, message: 'Courses loaded successfully',
                courses: mine.map(function (c) {
                    return {id: c.id, title: c.title, img: c.thumbnail, description: c.description,
                        categoryId: c.categoryId, categoryName: categoryName(c.categoryId),
                        difficulty: c.difficulty, pricelevel: c.pricelevel, price: c.price, statusId: 1,
                        statusName: c.status, createdAt: new Date(c.created_at).toString(),
                        instructor: instructorOf(c), university: categoryName(c.categoryId), rating: c.rating,
                        enrollmentCount: S.enrollments.filter(function (e) {
                            return e.courseId === c.id;
                        }).length, lessonCount: lessonsOf(c.id).length};
                })});
        },
        AddCourse: function (m, url, init) {
            var u = currentUser(), b = body(init);
            if (!u) {
                return ok({status: false, message: 'Please log in first.'});
            }
            var cat = byId(SEED.categories, b.category);
            if (!cat) {
                return ok({status: false, message: 'Failed to create course: Invalid Category!'});
            }
            var id = S.next.course++;
            S.courses.push({id: id, title: b.title, description: b.description, categoryId: cat.id,
                difficulty: b.difficulty, pricelevel: b.priceType, price: Number(b.priceLKR) || 0,
                instructorId: u.id, thumbnail: b.thumbnailUrl || '', rating: 4.5, status: 'Active',
                created_at: Date.now()});
            (b.lessons || []).forEach(function (l, i) {
                S.lessons.push({id: S.next.lesson++, courseId: id, title: l.title, video: l.videoUrl || SEED.videoPlaceholder,
                    resources: l.resourceUrl ? [l.resourceUrl] : [], order: i});
            });
            save();
            return ok({status: true, message: 'Course created successfully!'});
        },
        AdminReports: function (m, url) {
            var q = url.searchParams, action = q.get('action') || 'summary';
            var from = q.get('from') ? new Date(q.get('from') + 'T00:00:00').getTime() : null;
            var to = q.get('to') ? new Date(q.get('to') + 'T23:59:59').getTime() : null;
            var paid = S.orders.filter(function (o) {
                return o.status === 'Success';
            });
            var inRange = paid.filter(function (o) {
                return (from === null || o.created_at >= from) && (to === null || o.created_at <= to);
            });
            function sum(list) {
                return list.reduce(function (a, o) {
                    return a + o.total;
                }, 0);
            }
            if (action === 'summary') {
                return ok({status: true, data: {total_orders: paid.length, total_revenue: sum(paid),
                        range_orders: inRange.length, range_revenue: sum(inRange)}});
            }
            if (action === 'top-courses') {
                var agg = {};
                inRange.forEach(function (o) {
                    o.items.forEach(function (it) {
                        var a = agg[it.courseId] || (agg[it.courseId] = {course_id: it.courseId,
                            title: byId(S.courses, it.courseId).title, items_sold: 0, revenue: 0});
                        a.items_sold++;
                        a.revenue += it.price;
                    });
                });
                var list = Object.keys(agg).map(function (k) {
                    return agg[k];
                }).sort(function (a, b) {
                    return b.revenue - a.revenue;
                }).slice(0, parseInt(q.get('limit'), 10) || 10);
                return ok({status: true, data: {top_courses: list}});
            }
            if (action === 'users') {
                var n = S.users.filter(function (x) {
                    return (from === null || x.created_at >= from) && (to === null || x.created_at <= to);
                }).length;
                return ok({status: true, data: {new_users: n}});
            }
            return ok({status: false, message: 'Unknown action'});
        }
    };

    // ------------------------------------------------------- Cloudinary stub
    function shrinkImage(file, maxSide) {
        return new Promise(function (resolve) {
            var fr = new FileReader();
            fr.onerror = function () {
                resolve('');
            };
            fr.onload = function () {
                var img = new Image();
                img.onerror = function () {
                    resolve('');
                };
                img.onload = function () {
                    var s = Math.min(1, maxSide / Math.max(img.width, img.height));
                    var cv = document.createElement('canvas');
                    cv.width = Math.round(img.width * s);
                    cv.height = Math.round(img.height * s);
                    cv.getContext('2d').drawImage(img, 0, 0, cv.width, cv.height);
                    resolve(cv.toDataURL('image/jpeg', 0.8));
                };
                img.src = fr.result;
            };
            fr.readAsDataURL(file);
        });
    }

    function fakeUpload(init) {
        var file = init && init.body && init.body.get ? init.body.get('file') : null;
        var p;
        if (file && file.type && file.type.indexOf('image/') === 0) {
            p = shrinkImage(file, 480);
        } else if (file && file.type && file.type.indexOf('video/') === 0) {
            p = Promise.resolve(SEED.videoPlaceholder); // visitors' videos are not stored in the demo
        } else {
            p = Promise.resolve('demo-resource:' + (file && file.name ? file.name : 'resource.pdf'));
        }
        return p.then(function (url) {
            return {status: 200, body: {secure_url: url, url: url}};
        });
    }

    // ------------------------------------------------------- fetch intercept
    var realFetch = window.fetch ? window.fetch.bind(window) : null;
    window.fetch = function (input, init) {
        var raw = typeof input === 'string' ? input : (input && input.url) || '';
        var url;
        try {
            url = new URL(raw, location.href);
        } catch (e) {
            return realFetch(input, init);
        }
        var method = ((init && init.method) || (input && input.method) || 'GET').toUpperCase();
        var result;
        if (url.hostname === 'api.cloudinary.com') {
            result = fakeUpload(init);
        } else if (url.origin === location.origin) {
            var name = url.pathname.split('/').pop();
            if (routes.hasOwnProperty(name)) {
                try {
                    result = routes[name](method, url, init);
                } catch (e) {
                    result = {status: 500, body: {status: false, message: 'Demo error: ' + e.message}};
                }
            }
        }
        if (!result) {
            return realFetch(input, init);
        }
        return Promise.resolve(result).then(function (r) {
            return new Promise(function (resolve) {
                setTimeout(function () {
                    resolve(new Response(JSON.stringify(r.body), {status: r.status,
                        headers: {'Content-Type': 'application/json'}}));
                }, 70);
            });
        });
    };

    // ---------------------------------------------------- downloads (no server)
    function pdfBlob(title, lines) {
        function esc(s) {
            return String(s).replace(/[^\x20-\x7E]/g, '?').replace(/([\\()])/g, '\\$1');
        }
        var content = 'BT /F1 16 Tf 50 790 Td (' + esc(title) + ') Tj /F1 11 Tf 0 -26 Td 16 TL\n';
        lines.forEach(function (l) {
            content += '(' + esc(l) + ') Tj T*\n';
        });
        content += 'ET';
        var objs = ['<< /Type /Catalog /Pages 2 0 R >>', '<< /Type /Pages /Kids [3 0 R] /Count 1 >>',
            '<< /Type /Page /Parent 2 0 R /MediaBox [0 0 595 842] /Contents 4 0 R /Resources << /Font << /F1 5 0 R >> >> >>',
            '<< /Length ' + content.length + ' >>\nstream\n' + content + '\nendstream',
            '<< /Type /Font /Subtype /Type1 /BaseFont /Helvetica >>'];
        var out = '%PDF-1.4\n', offsets = [];
        objs.forEach(function (o, i) {
            offsets.push(out.length);
            out += (i + 1) + ' 0 obj\n' + o + '\nendobj\n';
        });
        var xref = out.length;
        out += 'xref\n0 ' + (objs.length + 1) + '\n0000000000 65535 f \n';
        offsets.forEach(function (o) {
            out += ('0000000000' + o).slice(-10) + ' 00000 n \n';
        });
        out += 'trailer\n<< /Size ' + (objs.length + 1) + ' /Root 1 0 R >>\nstartxref\n' + xref + '\n%%EOF';
        return new Blob([out], {type: 'application/pdf'});
    }

    function saveBlob(blob, filename) {
        var a = document.createElement('a');
        a.href = URL.createObjectURL(blob);
        a.download = filename;
        document.body.appendChild(a);
        a.click();
        setTimeout(function () {
            URL.revokeObjectURL(a.href);
            a.remove();
        }, 500);
    }

    document.addEventListener('click', function (e) {
        var a = e.target.closest ? e.target.closest('a[href^="DownloadResource"]') : null;
        if (!a) {
            return;
        }
        e.preventDefault();
        var q = new URL(a.getAttribute('href'), location.href).searchParams;
        var lesson = byId(S.lessons, q.get('lesson_id'));
        var res = lesson ? lesson.resources[Number(q.get('idx')) || 0] : null;
        if (!res) {
            return;
        }
        var name = res.replace('demo-resource:', '');
        var course = byId(S.courses, lesson.courseId);
        saveBlob(pdfBlob(name, ['SkillVerse demo resource', '', 'Course: ' + course.title, 'Lesson: ' + lesson.title, '',
            'This file is generated in your browser for the demo.',
            'In the full app this is a file uploaded by the instructor.']), /\.pdf$/i.test(name) ? name : name + '.pdf');
    }, true);

    document.addEventListener('DOMContentLoaded', function () {
        if (typeof window.downloadPdf === 'function') {
            window.downloadPdf = function (type) {
                var params = new URLSearchParams();
                var f = document.getElementById('from'), t = document.getElementById('to'), l = document.getElementById('limit');
                if (f && f.value) {
                    params.set('from', f.value);
                }
                if (t && t.value) {
                    params.set('to', t.value);
                }
                var base = location.origin + '/AdminReports?';
                function get(action, extra) {
                    var p = new URLSearchParams(params.toString());
                    p.set('action', action);
                    if (extra) {
                        p.set('limit', extra);
                    }
                    return routes.AdminReports('GET', new URL(base + p.toString())).body.data;
                }
                var lines = [];
                if (type === 'top-courses') {
                    get('top-courses', l ? l.value : 10).top_courses.forEach(function (r, i) {
                        lines.push((i + 1) + '. ' + r.title + '  sold: ' + r.items_sold + '  revenue: LKR ' + r.revenue.toFixed(2));
                    });
                } else if (type === 'users') {
                    lines.push('New users in range: ' + get('users').new_users);
                } else {
                    var s = get('summary');
                    lines.push('Total orders: ' + s.total_orders, 'Total revenue: LKR ' + s.total_revenue.toFixed(2),
                            'Orders in range: ' + s.range_orders, 'Revenue in range: LKR ' + s.range_revenue.toFixed(2));
                }
                saveBlob(pdfBlob('SkillVerse report: ' + type, lines.length ? lines : ['No data in this range.']),
                        'skillverse-' + type + '-report.pdf');
            };
        }
    });

    // ------------------------------------------------------ fake PayHere SDK
    window.payhere = {
        onCompleted: function () {
        }, onDismissed: function () {
        }, onError: function () {
        },
        startPayment: function (p) {
            var order = byId(S.orders, String(p.order_id).replace(/\D+/g, ''));
            var overlay = document.createElement('div');
            overlay.style.cssText = 'position:fixed;inset:0;background:rgba(10,14,25,.7);z-index:99999;display:flex;'
                    + 'align-items:center;justify-content:center;font-family:Montserrat,Arial,sans-serif;padding:16px';
            overlay.innerHTML = '<div style="background:#fff;color:#1b2333;border-radius:14px;max-width:380px;width:100%;'
                    + 'padding:24px;box-shadow:0 20px 60px rgba(0,0,0,.4)">'
                    + '<div style="font-size:12px;letter-spacing:.08em;color:#6b7280;text-transform:uppercase">PayHere sandbox (demo)</div>'
                    + '<h3 style="margin:6px 0 4px;font-size:20px">Confirm test payment</h3>'
                    + '<p style="margin:0 0 14px;color:#4b5563;font-size:14px">No real payment is taken. This simulates the PayHere popup.</p>'
                    + '<div style="background:#f3f4f6;border-radius:10px;padding:12px;font-size:14px;margin-bottom:16px">'
                    + '<div><b>Order</b> ' + p.order_id + '</div><div style="margin:4px 0"><b>Items</b> ' + p.items + '</div>'
                    + '<div><b>Total</b> ' + p.currency + ' ' + p.amount + '</div></div>'
                    + '<div style="display:flex;gap:10px"><button id="svPay" style="flex:1;border:0;border-radius:10px;padding:11px;'
                    + 'background:#006bff;color:#fff;font-weight:700;cursor:pointer">Pay (demo)</button>'
                    + '<button id="svCancel" style="flex:1;border:1px solid #d1d5db;border-radius:10px;padding:11px;'
                    + 'background:#fff;color:#1b2333;font-weight:600;cursor:pointer">Cancel</button></div></div>';
            document.body.appendChild(overlay);
            overlay.querySelector('#svPay').onclick = function () {
                overlay.remove();
                if (order) {
                    fulfilOrder(order);
                }
                window.payhere.onCompleted(p.order_id);
            };
            overlay.querySelector('#svCancel').onclick = function () {
                overlay.remove();
                window.payhere.onDismissed();
            };
        }
    };

    // ------------------------------------------------------------------ banner
    function addBanner() {
        var wrap = document.createElement('div');
        wrap.style.cssText = 'position:fixed;left:12px;bottom:12px;z-index:99998;font:13px Montserrat,Arial,sans-serif';
        wrap.innerHTML = '<div id="svPanel" style="display:none;background:#fff;color:#1b2333;border-radius:12px;padding:14px;'
                + 'margin-bottom:8px;width:300px;box-shadow:0 10px 40px rgba(0,0,0,.3)">'
                + '<div style="font-weight:700;margin-bottom:6px">Demo logins</div>'
                + '<div style="margin-bottom:8px"><b>Learner</b> (owns 3 courses)<br><code>learner@skillverse.demo</code><br><code>Learner@123</code></div>'
                + '<div style="margin-bottom:10px"><b>Instructor / reports</b><br><code>instructor@skillverse.demo</code><br><code>Teacher@123</code></div>'
                + '<div style="display:flex;gap:6px;flex-wrap:wrap">'
                + '<button data-fill="learner" style="border:0;border-radius:8px;padding:6px 10px;background:#006bff;color:#fff;cursor:pointer">Fill learner</button>'
                + '<button data-fill="instructor" style="border:0;border-radius:8px;padding:6px 10px;background:#006bff;color:#fff;cursor:pointer">Fill instructor</button>'
                + '<button id="svReset" style="border:1px solid #d1d5db;border-radius:8px;padding:6px 10px;background:#fff;cursor:pointer">Reset demo data</button></div>'
                + '<div style="margin-top:8px;color:#6b7280;font-size:12px">Sandbox payments. Data stays in your browser. Sign-up code: 123456.</div></div>'
                + '<button id="svToggle" style="border:0;border-radius:999px;padding:8px 14px;background:#0f172a;color:#fff;'
                + 'cursor:pointer;box-shadow:0 4px 16px rgba(0,0,0,.3)">Demo build &middot; logins</button>';
        document.body.appendChild(wrap);
        var panel = wrap.querySelector('#svPanel');
        wrap.querySelector('#svToggle').onclick = function () {
            panel.style.display = panel.style.display === 'none' ? 'block' : 'none';
        };
        wrap.querySelector('#svReset').onclick = function () {
            if (confirm('Reset all demo data (accounts you created, carts, courses, purchases)?')) {
                try {
                    localStorage.removeItem(KEY);
                    localStorage.removeItem('sv_uid');
                    localStorage.removeItem('sv_pending');
                } catch (e) {
                }
                location.href = 'index.html';
            }
        };
        wrap.querySelectorAll('[data-fill]').forEach(function (b) {
            b.onclick = function () {
                var learner = b.getAttribute('data-fill') === 'learner';
                var em = document.getElementById('loginEmail'), pw = document.getElementById('loginPassword');
                if (em && pw) {
                    em.value = learner ? 'learner@skillverse.demo' : 'instructor@skillverse.demo';
                    pw.value = learner ? 'Learner@123' : 'Teacher@123';
                    var modal = document.getElementById('loginModal');
                    if (modal && window.bootstrap) {
                        window.bootstrap.Modal.getOrCreateInstance(modal).show();
                    }
                } else {
                    alert('Open the landing page to sign in with these accounts.');
                }
            };
        });
    }
    document.addEventListener('DOMContentLoaded', addBanner);
})();
